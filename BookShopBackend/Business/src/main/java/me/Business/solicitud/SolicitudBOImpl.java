package me.Business.solicitud;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import me.Business.compra.PedidoBOImpl;
import me.Business.compra.ProveedorConfiguracion;
import me.Business.configuracion.ConfiguracionBOImpl;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.FueraPlazoException;
import me.Business.exceptions.LimiteIntentosException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
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

public class SolicitudBOImpl implements SolicitudBO {
    private final SolicitudDAO solicitudDAO;
    private final PedidoDAO pedidoDAO;
    private final ResenaDAO resenaDAO;
    private final LibroDAO libroDAO;
    private final CuponDAO cuponDAO;
    private final Autorizacion autorizacion;
    private final Clock reloj;
    private final ProveedorConfiguracion configuracion;
    private final PedidoBOImpl pedidoBO;

    public SolicitudBOImpl() {
        this(new SolicitudDAOImpl(), new PedidoDAOImpl(), new ResenaDAOImpl(), new LibroDAOImpl(), new CuponDAOImpl(),
            new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")), ConfiguracionBOImpl::actual, new PedidoBOImpl());
    }

    public SolicitudBOImpl(SolicitudDAO solicitudDAO, PedidoDAO pedidoDAO, ResenaDAO resenaDAO, LibroDAO libroDAO,
        CuponDAO cuponDAO, UsuarioDAO usuarioDAO, Clock reloj, ProveedorConfiguracion configuracion, PedidoBOImpl pedidoBO) {
        this.solicitudDAO = solicitudDAO;
        this.pedidoDAO = pedidoDAO;
        this.resenaDAO = resenaDAO;
        this.libroDAO = libroDAO;
        this.cuponDAO = cuponDAO;
        this.autorizacion = new Autorizacion(usuarioDAO);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
        this.configuracion = configuracion;
        this.pedidoBO = pedidoBO;
    }

    @Override
    public void comprobarDevolucion(int idActor, int idPedido) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        try {
            validarDevolucion(pedidoDAO.leer(idPedido), idActor, configuracion.obtener(), LocalDateTime.now(reloj));
        }
        catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar la devolucion.", e);
        }
    }

    private void validarDevolucion(Pedido pedido, int idActor, ConfiguracionDTO parametros, LocalDateTime fecha) throws SQLException, NegocioException {
        if (pedido == null || pedido.isEliminado()) throw new RecursoNoEncontradoException("El pedido no existe.");
        autorizacion.propietario(idActor, pedido.getUsuario().getId());
        if (pedido.getEstado() != EstadoPedido.ENTREGADO || pedido.getFechaEntrega() == null)
            throw new ConflictoException("Solo puedes solicitar devolucion de un pedido entregado.");
        if (fecha.isBefore(pedido.getFechaEntrega()) || fecha.isAfter(pedido.getFechaEntrega().plusDays(parametros.plazoDiasDevolucion())))
            throw new FueraPlazoException("El pedido esta fuera del plazo de devolucion.");
        if (pedido.getNumeroIntentos() >= parametros.maximoIntentosDevolucion())
            throw new LimiteIntentosException("Se alcanzo el maximo de solicitudes de devolucion.");
        if (solicitudDAO.existeDevolucionPendiente(pedido.getId()))
            throw new ConflictoException("El pedido ya tiene una devolucion pendiente.");
    }

    @Override
    public void comprobarReporte(int idActor, int idResena) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        try {
            validarReporte(resenaDAO.leer(idResena), idActor);
        }
        catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar el reporte.", e);
        }
    }

    private void validarReporte(Resena resena, int idActor) throws SQLException, NegocioException {
        if (resena == null || !resena.estaDisponible()) throw new RecursoNoEncontradoException("La resena ya no esta disponible.");
        if (solicitudDAO.existeReportePendiente(idActor, resena.getId()))
            throw new ConflictoException("Ya tienes un reporte pendiente de esta resena.");
    }

    @Override
    public DetalleDevolucionDTO enviarDevolucion(int idActor, int idPedido, DatosDevolucionDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        String comentario = validarDatosDevolucion(datos);
        SolicitudDevolucion solicitud;
        try {
            TransactionContext.getConnection();
            Pedido pedido = pedidoDAO.leerParaActualizar(idPedido);
            ConfiguracionDTO parametros = configuracion.obtener();
            LocalDateTime ahora = LocalDateTime.now(reloj);
            validarDevolucion(pedido, idActor, parametros, ahora);
            if (!pedidoDAO.incrementarIntentos(idPedido, parametros.maximoIntentosDevolucion(), actor.getEmail()))
                throw new ConflictoException("El pedido cambio antes de enviar la solicitud.");
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

    private String validarDatosDevolucion(DatosDevolucionDTO datos) throws NegocioException {
        if (datos == null || datos.motivo() == null) throw new ValidacionException("motivo", "Selecciona un motivo de devolucion.");
        String comentario = datos.comentario();
        if (comentario == null || comentario.isBlank()) return null;
        if (comentario.length() > 5000) throw new ValidacionException("comentario", "El comentario admite como maximo 5000 caracteres.");
        return comentario.trim();
    }

    private String validarDatosReporte(DatosReporteResenaDTO datos) throws NegocioException {
        if (datos == null || datos.motivo() == null) throw new ValidacionException("motivo", "Selecciona un motivo de reporte.");
        String comentario = datos.comentario();
        if (comentario == null || comentario.isBlank()) return null;
        if (comentario.length() > 5000) throw new ValidacionException("comentario", "El comentario admite como maximo 5000 caracteres.");
        return comentario.trim();
    }

    @Override
    public DetalleReporteResenaDTO enviarReporte(int idActor, int idResena, DatosReporteResenaDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        String comentario = validarDatosReporte(datos);
        SolicitudReporteResena solicitud;
        try {
            TransactionContext.getConnection();
            Resena resena = resenaDAO.leerParaActualizar(idResena);
            validarReporte(resena, idActor);
            solicitud = new SolicitudReporteResena(0, actor, resena, datos.motivo(), comentario);
            solicitud.setFechaCreacion(LocalDateTime.now(reloj));
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

    private Solicitud obtenerSolicitudPendiente(int idActor, int idSolicitud) throws SQLException, NegocioException {
        Solicitud solicitud = solicitudDAO.leer(idSolicitud);
        if (solicitud == null || solicitud.isEliminado()) throw new RecursoNoEncontradoException("La solicitud no existe.");
        autorizacion.propietario(idActor, solicitud.getUsuario().getId());
        if (!solicitud.estaPendiente()) throw new ConflictoException("Solo puedes editar o retirar solicitudes pendientes.");
        return solicitud;
    }

    @Override
    public DetalleDevolucionDTO editarDevolucion(int idActor, int idSolicitud, DatosDevolucionDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        String comentario = validarDatosDevolucion(datos);
        try {
            Solicitud solicitud = obtenerSolicitudPendiente(idActor, idSolicitud);
            if (!(solicitud instanceof SolicitudDevolucion devolucion)) throw new ConflictoException("La solicitud no es una devolucion.");
            devolucion.editar(datos.motivo(), comentario, actor.getEmail(), LocalDateTime.now(reloj));
            // Una sola actualización condicionada; no abre transacción ni suma otro intento.
            solicitudDAO.modificar(devolucion);
            return consultarDevolucion(idActor, idSolicitud);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo editar la devolucion; puede haber sido resuelta.", e);
        }
    }

    @Override
    public DetalleReporteResenaDTO editarReporte(int idActor, int idSolicitud, DatosReporteResenaDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        String comentario = validarDatosReporte(datos);
        try {
            Solicitud solicitud = obtenerSolicitudPendiente(idActor, idSolicitud);
            if (!(solicitud instanceof SolicitudReporteResena reporte)) throw new ConflictoException("La solicitud no es un reporte.");
            reporte.editar(datos.motivo(), comentario, actor.getEmail(), LocalDateTime.now(reloj));
            solicitudDAO.modificar(reporte);
            return consultarReporte(idActor, idSolicitud);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo editar el reporte; puede haber sido resuelto.", e);
        }
    }

    @Override
    public void retirar(int idActor, int idSolicitud) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            obtenerSolicitudPendiente(idActor, idSolicitud);
            if (!solicitudDAO.eliminar(idSolicitud, actor.getEmail())) throw new ConflictoException("La solicitud ya no esta pendiente.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo retirar la solicitud.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<ResumenSolicitudDTO> listarPropias(int idActor, FiltroSolicitudDTO filtro) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        return listar(idActor, filtro);
    }

    @Override
    public ResultadoPaginadoDTO<ResumenSolicitudDTO> listarModeracion(int idActor, FiltroSolicitudDTO filtro) throws NegocioException {
        autorizacion.administrador(idActor);
        return listar(0, filtro);
    }

    private ResultadoPaginadoDTO<ResumenSolicitudDTO> listar(int propietario, FiltroSolicitudDTO filtro) throws NegocioException {
        if (filtro == null) filtro = FiltroSolicitudDTO.porDefecto();
        if (filtro.tipo() == null) throw new ValidacionException("tipo", "Selecciona la pestana de solicitudDAO.");
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La pagina y su tamaño deben ser positivos.");
        try {
            return solicitudDAO.listar(propietario, filtro);
        }
        catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las solicitudDAO.", e);
        }
    }

    private Solicitud obtenerSolicitudParaConsulta(int idActor, int idSolicitud) throws SQLException, NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        Solicitud solicitud = solicitudDAO.leer(idSolicitud);
        if (solicitud == null || solicitud.isEliminado()) throw new RecursoNoEncontradoException("La solicitud no existe.");
        if (actor.getId() != solicitud.getUsuario().getId()) autorizacion.administrador(idActor);
        return solicitud;
    }

    @Override
    public DetalleDevolucionDTO consultarDevolucion(int idActor, int idSolicitud) throws NegocioException {
        try {
            Solicitud solicitud = obtenerSolicitudParaConsulta(idActor, idSolicitud);
            if (!(solicitud instanceof SolicitudDevolucion))
                throw new ConflictoException("La solicitud no es una devolucion.");
            return solicitudDAO.obtenerDetalleDevolucion(idSolicitud);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la devolucion.", e);
        }
    }

    @Override
    public DetalleReporteResenaDTO consultarReporte(int idActor, int idSolicitud) throws NegocioException {
        try {
            Solicitud solicitud = obtenerSolicitudParaConsulta(idActor, idSolicitud);
            if (!(solicitud instanceof SolicitudReporteResena))
                throw new ConflictoException("La solicitud no es un reporte.");
            boolean administrador = autorizacion.usuarioActivo(idActor).getRol() == Rol.ADMIN;
            return solicitudDAO.obtenerDetalleReporteResena(idSolicitud, administrador);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el reporte.", e);
        }
    }

    @Override
    public DetallePedidoDTO consultarPedidoReferencia(int idActor, int idSolicitud) throws NegocioException {
        try {
            Solicitud solicitud = obtenerSolicitudParaConsulta(idActor, idSolicitud);
            if (!(solicitud instanceof SolicitudDevolucion devolucion)) throw new ConflictoException("La solicitud no referencia un pedido.");
            return pedidoBO.consultarReferencia(idActor, devolucion.getPedido().getId());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el pedido de la solicitud.", e);
        }
    }

    @Override
    public DetalleDevolucionDTO resolverDevolucion(int idActor, int idSolicitud, EstadoSolicitud resultado) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (resultado != EstadoSolicitud.ACEPTADA && resultado != EstadoSolicitud.RECHAZADA)
            throw new ValidacionException("resultado", "Una devolucion se acepta o rechaza.");
        try {
            TransactionContext.getConnection();
            Solicitud solicitud = solicitudDAO.leerParaActualizar(idSolicitud);
            if (!(solicitud instanceof SolicitudDevolucion devolucion) || !solicitud.estaPendiente())
                throw new ConflictoException("No existe una devolucion pendiente para resolver.");
            LocalDateTime ahora = LocalDateTime.now(reloj);
            Pedido pedido = pedidoDAO.leerParaActualizar(devolucion.getPedido().getId());
            if (pedido == null || pedido.getEstado() != EstadoPedido.ENTREGADO)
                throw new ConflictoException("El pedido ya no admite devolucion.");
            // Una solicitud válida pendiente conserva su elegibilidad aunque cambien plazo/configuración.
            if (!solicitudDAO.resolver(idSolicitud, resultado, ahora, actor.getEmail())) throw new ConflictoException("La solicitud ya fue resuelta.");
            if (resultado == EstadoSolicitud.ACEPTADA) {
                pedido.setEstado(EstadoPedido.DEVUELTO);
                pedido.setFechaActualizacion(ahora);
                pedido.setUsuarioActualizacion(actor.getEmail());
                if (!pedidoDAO.actualizarEstado(pedido, EstadoPedido.ENTREGADO)) throw new ConflictoException("El pedido ya cambio de estado.");
                List<Item> items = pedido.getItems();
                items.sort(Comparator.comparingInt(item -> item.getLibro().getId()));
                for (Item item : items)
                    if (!libroDAO.reponerStock(item.getLibro().getId(), item.getCantidad(), actor.getEmail()))
                        throw new ConflictoException("No se pudo reponer el stock.");
                if (pedido.getCupon() != null) cuponDAO.liberar(pedido.getCupon().getId(), ahora, actor.getEmail());
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
    public DetalleReporteResenaDTO resolverReporte(int idActor, int idSolicitud, EstadoSolicitud resultado) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (resultado == null || resultado == EstadoSolicitud.PENDIENTE)
            throw new ValidacionException("resultado", "Selecciona el resultado del reporte.");
        try {
            TransactionContext.getConnection();
            Solicitud solicitud = solicitudDAO.leerParaActualizar(idSolicitud);
            if (!(solicitud instanceof SolicitudReporteResena reporte) || !solicitud.estaPendiente())
                throw new ConflictoException("No existe un reporte pendiente para resolver.");
            Resena resena = resenaDAO.leerParaActualizar(reporte.getResena().getId());
            if (resultado == EstadoSolicitud.ACEPTADA && (resena == null || !resena.estaDisponible()))
                throw new ConflictoException("La resena ya fue retirada; puedes cerrar la solicitud.");
            if (resultado == EstadoSolicitud.CERRADA && resena != null && resena.estaDisponible()
                && resena.getPuntuacion() == reporte.getPuntuacionOriginal()
                && Objects.equals(resena.getComentario(), reporte.getComentarioOriginal()))
                throw new ConflictoException("El contenido original sigue publicado; acepta o rechaza el reporte.");
            if (!solicitudDAO.resolver(idSolicitud, resultado, LocalDateTime.now(reloj), actor.getEmail()))
                throw new ConflictoException("La solicitud ya fue resuelta.");
            if (resultado == EstadoSolicitud.ACEPTADA && !resenaDAO.eliminar(resena.getId(), actor.getEmail()))
                throw new ConflictoException("La resena ya fue retirada.");
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
}
