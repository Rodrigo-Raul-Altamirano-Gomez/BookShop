package me.Business.solicitud;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import me.Business.compra.PedidoBOImpl;
import me.Business.compra.PedidoBO;
import me.Business.configuracion.ConfiguracionBOImpl;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.catalogo.ResenaDAO;
import me.DAO.catalogo.ResenaDAOImpl;
import me.DAO.solicitud.SolicitudDAO;
import me.DAO.solicitud.SolicitudDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DAO.venta.CuponDAO;
import me.DAO.venta.CuponDAOImpl;
import me.DAO.venta.PedidoDAO;
import me.DAO.venta.PedidoDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ConfiguracionDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.solicitud.DatosDevolucionDTO;
import me.Model.dto.solicitud.DatosReporteResenaDTO;
import me.Model.dto.solicitud.DetalleDevolucionDTO;
import me.Model.dto.solicitud.DetalleReporteResenaDTO;
import me.Model.dto.solicitud.FiltroSolicitudDTO;
import me.Model.dto.solicitud.ResumenSolicitudDTO;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.compra.Item;
import me.Model.entities.compra.Pedido;
import me.Model.entities.opinion.Resena;
import me.Model.entities.solicitud.EstadoSolicitud;
import me.Model.entities.solicitud.Solicitud;
import me.Model.entities.solicitud.SolicitudDevolucion;
import me.Model.entities.solicitud.SolicitudReporteResena;
import me.Model.entities.usuario.Rol;
import me.Model.entities.usuario.Usuario;
import me.Business.exceptions.AccesoDenegadoException;

public class SolicitudBOImpl implements SolicitudBO {

    private final SolicitudDAO solicitudDAO = new SolicitudDAOImpl();
    private final PedidoDAO pedidoDAO = new PedidoDAOImpl();
    private final ResenaDAO resenaDAO = new ResenaDAOImpl();
    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final CuponDAO cuponDAO = new CuponDAOImpl();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final PedidoBO pedidoBO = new PedidoBOImpl();

    @Override
    public void comprobarDevolucion(int idActor, int idPedido) throws NegocioException {
        try {
            validarDevolucion(pedidoDAO.leer(idPedido), idActor, new ConfiguracionBOImpl().obtener(),
                LocalDateTime.now());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar la devolucion.", e);
        }
    }

    private void validarDevolucion(Pedido pedido, int idActor, ConfiguracionDTO parametros, LocalDateTime
        fecha) throws SQLException, NegocioException {
        if (pedido == null || pedido.isEliminado())
            throw new RecursoNoEncontradoException("El pedido no existe.");
        if (pedido.getUsuario().getId() != idActor)
            throw new AccesoDenegadoException("El pedido pertenece a otra cuenta.");
        if (pedido.getEstado() != EstadoPedido.ENTREGADO || pedido.getFechaEntrega() == null)
            throw new NegocioException("Solo puedes solicitar devolucion de un pedido entregado.");
        if (fecha.isAfter(pedido.getFechaEntrega().plusDays(parametros.plazoDiasDevolucion())))
            throw new NegocioException("El pedido esta fuera del plazo de devolucion.");
        if (pedido.getNumeroIntentos() >= parametros.maximoIntentosDevolucion())
            throw new NegocioException("Se alcanzo el maximo de solicitudes de devolucion.");
        if (solicitudDAO.existeDevolucionPendiente(pedido.getId()))
            throw new NegocioException("El pedido ya tiene una devolucion pendiente.");
    }

    @Override
    public void comprobarReporte(int idActor, int idResena) throws NegocioException {
        try {
            Resena resena = resenaDAO.leer(idResena);
            if (resena == null || !resena.estaDisponible())
                throw new RecursoNoEncontradoException("La resena ya no esta disponible.");
            if (solicitudDAO.existeReportePendiente(idActor, resena.getId()))
                throw new NegocioException("Ya tienes un reporte pendiente de esta resena.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar el reporte.", e);
        }
    }

    @Override
    public DetalleDevolucionDTO enviarDevolucion(int idActor, int idPedido, DatosDevolucionDTO datos) throws
        NegocioException {
        SolicitudDevolucion solicitud;
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (datos == null || datos.motivo() == null)
                throw new ValidacionException("motivo", "Selecciona un motivo de devolucion.");
            String comentario = validarComentario(datos.comentario());
            TransactionContext.getConnection();
            Pedido pedido = pedidoDAO.leer(idPedido);
            ConfiguracionDTO parametros = new ConfiguracionBOImpl().obtener();
            LocalDateTime ahora = LocalDateTime.now();
            validarDevolucion(pedido, idActor, parametros, ahora);
            if (!pedidoDAO.incrementarIntentos(idPedido, parametros.maximoIntentosDevolucion(), actor.getEmail()))
                throw new NegocioException("El pedido cambio antes de enviar la solicitud.");
            solicitud = new SolicitudDevolucion(0, actor, pedido, datos.motivo(), comentario);
            solicitud.setFechaCreacion(ahora);
            solicitud.setUsuarioCreacion(actor.getEmail());
            solicitudDAO.insertar(solicitud);
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo enviar la devolucion. No se consumio el intento.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
        return consultarDevolucion(idActor, solicitud.getId());
    }

    @Override
    public DetalleReporteResenaDTO enviarReporte(int idActor, int idResena, DatosReporteResenaDTO datos)
        throws NegocioException {
        SolicitudReporteResena solicitud;
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (datos == null || datos.motivo() == null)
                throw new ValidacionException("motivo", "Selecciona un motivo de reporte.");
            String comentario = validarComentario(datos.comentario());
            TransactionContext.getConnection();
            Resena resena = resenaDAO.leer(idResena);
            if (resena == null || !resena.estaDisponible())
                throw new RecursoNoEncontradoException("La resena ya no esta disponible.");
            if (solicitudDAO.existeReportePendiente(idActor, resena.getId()))
                throw new NegocioException("Ya tienes un reporte pendiente de esta resena.");
            solicitud = new SolicitudReporteResena(0, actor, resena, datos.motivo(), comentario);
            solicitud.setFechaCreacion(LocalDateTime.now());
            solicitud.setUsuarioCreacion(actor.getEmail());
            solicitudDAO.insertar(solicitud);
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo enviar el reporte.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
        return consultarReporte(idActor, solicitud.getId());
    }

    @Override
    public DetalleDevolucionDTO editarDevolucion(int idActor, int idSolicitud, DatosDevolucionDTO datos)
        throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (datos == null || datos.motivo() == null)
                throw new ValidacionException("motivo", "Selecciona un motivo de devolucion.");
            String comentario = validarComentario(datos.comentario());
            Solicitud solicitud = leerPendientePropia(idActor, idSolicitud);
            if (!(solicitud instanceof SolicitudDevolucion))
                throw new NegocioException("La solicitud no es una devolucion.");
            SolicitudDevolucion devolucion = (SolicitudDevolucion) solicitud;
            devolucion.editar(datos.motivo(), comentario, actor.getEmail(), LocalDateTime.now());
            // Una sola actualización condicionada; no abre transacción ni suma otro intento.
            solicitudDAO.modificar(devolucion);
            return consultarDevolucion(idActor, idSolicitud);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo editar la devolucion; puede haber sido resuelta.", e);
        }
    }

    @Override
    public DetalleReporteResenaDTO editarReporte(int idActor, int idSolicitud, DatosReporteResenaDTO datos)
        throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (datos == null || datos.motivo() == null)
                throw new ValidacionException("motivo", "Selecciona un motivo de reporte.");
            String comentario = validarComentario(datos.comentario());
            Solicitud solicitud = leerPendientePropia(idActor, idSolicitud);
            if (!(solicitud instanceof SolicitudReporteResena))
                throw new NegocioException("La solicitud no es un reporte.");
            SolicitudReporteResena reporte = (SolicitudReporteResena) solicitud;
            reporte.editar(datos.motivo(), comentario, actor.getEmail(), LocalDateTime.now());
            solicitudDAO.modificar(reporte);
            return consultarReporte(idActor, idSolicitud);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo editar el reporte; puede haber sido resuelto.", e);
        }
    }

    @Override
    public void retirar(int idActor, int idSolicitud) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Solicitud solicitud = leerPendientePropia(idActor, idSolicitud);
            if (!solicitudDAO.eliminar(idSolicitud, actor.getEmail()))
                throw new NegocioException("La solicitud ya no esta pendiente.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo retirar la solicitud.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<ResumenSolicitudDTO> listarPropias(int idActor, FiltroSolicitudDTO filtro)
        throws NegocioException {
        filtro = validarFiltro(filtro);
        try {
            return solicitudDAO.listar(idActor, filtro);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las solicitudes.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<ResumenSolicitudDTO> listarModeracion(FiltroSolicitudDTO filtro)
        throws NegocioException {
        filtro = validarFiltro(filtro);
        try {
            return solicitudDAO.listar(0, filtro);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las solicitudes.", e);
        }
    }

    @Override
    public DetalleDevolucionDTO consultarDevolucion(int idActor, int idSolicitud) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Solicitud solicitud = solicitudDAO.leer(idSolicitud);
            comprobarAcceso(solicitud, actor);
            if (!(solicitud instanceof SolicitudDevolucion))
                throw new NegocioException("La solicitud no es una devolucion.");
            return solicitudDAO.obtenerDetalleDevolucion(idSolicitud);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la devolucion.", e);
        }
    }

    @Override
    public DetalleReporteResenaDTO consultarReporte(int idActor, int idSolicitud) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Solicitud solicitud = solicitudDAO.leer(idSolicitud);
            comprobarAcceso(solicitud, actor);
            if (!(solicitud instanceof SolicitudReporteResena))
                throw new NegocioException("La solicitud no es un reporte.");
            boolean administrador = actor.getRol() == Rol.ADMIN;
            return solicitudDAO.obtenerDetalleReporteResena(idSolicitud, administrador);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el reporte.", e);
        }
    }

    @Override
    public DetallePedidoDTO consultarPedidoReferencia(int idActor, int idSolicitud) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Solicitud solicitud = solicitudDAO.leer(idSolicitud);
            comprobarAcceso(solicitud, actor);
            if (!(solicitud instanceof SolicitudDevolucion))
                throw new NegocioException("La solicitud no referencia un pedido.");
            SolicitudDevolucion devolucion = (SolicitudDevolucion) solicitud;
            return pedidoBO.consultarReferencia(devolucion.getPedido().getId());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el pedido de la solicitud.", e);
        }
    }

    @Override
    public DetalleDevolucionDTO resolverDevolucion(int idActor, int idSolicitud, EstadoSolicitud resultado)
        throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (resultado != EstadoSolicitud.ACEPTADA && resultado != EstadoSolicitud.RECHAZADA)
                throw new ValidacionException("resultado", "Una devolucion se acepta o rechaza.");
            TransactionContext.getConnection();
            Solicitud solicitud = solicitudDAO.leer(idSolicitud);
            if (!(solicitud instanceof SolicitudDevolucion) || !solicitud.estaPendiente())
                throw new NegocioException("No existe una devolucion pendiente para resolver.");
            SolicitudDevolucion devolucion = (SolicitudDevolucion) solicitud;
            LocalDateTime ahora = LocalDateTime.now();
            Pedido pedido = pedidoDAO.leer(devolucion.getPedido().getId());
            if (pedido == null || pedido.getEstado() != EstadoPedido.ENTREGADO)
                throw new NegocioException("El pedido ya no admite devolucion.");
            // Una solicitud válida pendiente conserva su elegibilidad aunque cambien plazo/configuración.
            if (!solicitudDAO.resolver(idSolicitud, resultado, ahora, actor.getEmail()))
                throw new NegocioException("La solicitud ya fue resuelta.");
            if (resultado == EstadoSolicitud.ACEPTADA) {
                pedido.setEstado(EstadoPedido.DEVUELTO);
                pedido.setFechaActualizacion(ahora);
                pedido.setUsuarioActualizacion(actor.getEmail());
                if (!pedidoDAO.actualizarEstado(pedido, EstadoPedido.ENTREGADO))
                    throw new NegocioException("El pedido ya cambio de estado.");
                List<Item> items = pedido.getItems();
                for (Item item : items) if (!libroDAO.reponerStock(item.getLibro().getId(),
                    item.getCantidad(), actor.getEmail()))
                    throw new NegocioException("No se pudo reponer el stock.");
                if (pedido.getCupon() != null)
                    cuponDAO.liberar(pedido.getCupon().getId(), ahora, actor.getEmail());
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo resolver la devolucion.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
        return consultarDevolucion(idActor, idSolicitud);
    }

    @Override
    public DetalleReporteResenaDTO resolverReporte(int idActor, int idSolicitud, EstadoSolicitud resultado)
        throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (resultado == null || resultado == EstadoSolicitud.PENDIENTE)
                throw new ValidacionException("resultado", "Selecciona el resultado del reporte.");
            TransactionContext.getConnection();
            Solicitud solicitud = solicitudDAO.leer(idSolicitud);
            if (!(solicitud instanceof SolicitudReporteResena) || !solicitud.estaPendiente())
                throw new NegocioException("No existe un reporte pendiente para resolver.");
            SolicitudReporteResena reporte = (SolicitudReporteResena) solicitud;
            Resena resena = resenaDAO.leer(reporte.getResena().getId());
            if (resultado == EstadoSolicitud.ACEPTADA && (resena == null || !resena.estaDisponible()))
                throw new NegocioException("La resena ya fue retirada; puedes cerrar la solicitud.");
            if (resultado == EstadoSolicitud.CERRADA && resena != null && resena.estaDisponible() &&
                resena.getPuntuacion() == reporte.getPuntuacionOriginal() &&
                Objects.equals(resena.getComentario(), reporte.getComentarioOriginal()))
                throw new NegocioException("El contenido original sigue publicado; acepta o rechaza el" +
                    " reporte.");
            if (!solicitudDAO.resolver(idSolicitud, resultado, LocalDateTime.now(), actor.getEmail()))
                throw new NegocioException("La solicitud ya fue resuelta.");
            if (resultado == EstadoSolicitud.ACEPTADA && !resenaDAO.eliminar(resena.getId(), actor.getEmail()))
                throw new NegocioException("La resena ya fue retirada.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo resolver el reporte.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
        return consultarReporte(idActor, idSolicitud);
    }

    // Lee una solicitud que debe ser del usuario y seguir pendiente para poder editarla o retirarla.
    private Solicitud leerPendientePropia(int idActor, int idSolicitud) throws SQLException, NegocioException {
        Solicitud solicitud = solicitudDAO.leer(idSolicitud);
        if (solicitud == null || solicitud.isEliminado())
            throw new RecursoNoEncontradoException("La solicitud no existe.");
        if (solicitud.getUsuario().getId() != idActor)
            throw new AccesoDenegadoException("La solicitud pertenece a otra cuenta.");
        if (!solicitud.estaPendiente())
            throw new NegocioException("Solo puedes editar o retirar solicitudes pendientes.");
        return solicitud;
    }

    private FiltroSolicitudDTO validarFiltro(FiltroSolicitudDTO filtro) throws ValidacionException {
        if (filtro == null)
            filtro = FiltroSolicitudDTO.porDefecto();
        if (filtro.tipo() == null)
            throw new ValidacionException("tipo", "Selecciona la pestaña de solicitudes.");
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La pagina y su tamaño deben ser positivos.");
        return filtro;
    }

    private String validarComentario(String comentario) {
        if (comentario == null || comentario.isBlank())
            return null;
        return comentario.strip();
    }

    private void comprobarAcceso(Solicitud solicitud, Usuario actor) throws NegocioException {
        if (solicitud == null || solicitud.isEliminado())
            throw new RecursoNoEncontradoException("La solicitud no existe.");
        if (actor == null || !actor.estaDisponible() ||
            (actor.getId() != solicitud.getUsuario().getId() && actor.getRol() != Rol.ADMIN))
            throw new AccesoDenegadoException("La solicitud pertenece a otra cuenta.");
    }

}
