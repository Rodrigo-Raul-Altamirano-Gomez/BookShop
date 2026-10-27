package me.Business.opinion;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.Business.Validaciones;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.catalogo.ResenaDAO;
import me.DAO.catalogo.ResenaDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DAO.venta.PedidoDAO;
import me.DAO.venta.PedidoDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.opinion.ConsultaResenasDTO;
import me.Model.dto.opinion.DatosResenaDTO;
import me.Model.dto.opinion.OrdenResena;
import me.Model.dto.opinion.ResenaDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.opinion.Resena;
import me.Model.entities.usuario.Usuario;

public class ResenaBOImpl implements ResenaBO {
    private final ResenaDAO resenas;
    private final LibroDAO libros;
    private final PedidoDAO pedidos;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public ResenaBOImpl() {
        this(new ResenaDAOImpl(), new LibroDAOImpl(), new PedidoDAOImpl(), new UsuarioDAOImpl(),
                Clock.system(ZoneId.of("America/Lima")));
    }

    public ResenaBOImpl(ResenaDAO resenas, LibroDAO libros, PedidoDAO pedidos, UsuarioDAO usuarios, Clock reloj) {
        this.resenas = resenas;
        this.libros = libros;
        this.pedidos = pedidos;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public void verificarPuedePublicar(int idActor, int idLibro) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        try { comprobarPublicacion(idActor, idLibro); }
        catch (SQLException e) { throw new PersistenciaException("No se pudo comprobar si puede publicar una resena.", e); }
    }

    private void comprobarPublicacion(int idActor, int idLibro) throws SQLException, NegocioException {
        Libro libro = libros.leer(idLibro);
        if (libro == null || !libro.isActivo()) throw new RecursoNoEncontradoException("El libro no esta disponible.");
        if (resenas.existePublicada(idActor, idLibro)) throw new ConflictoException("Ya tiene una resena publicada para este libro.");
        if (!pedidos.haCompradoLibro(idActor, idLibro))
            throw new AccesoDenegadoException("Necesita una compra confirmada que no este cancelada ni devuelta.");
    }

    @Override
    public int publicar(int idActor, int idLibro, DatosResenaDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        DatosResenaDTO validos = validarDatos(datos);
        try {
            comprobarPublicacion(idActor, idLibro);
            if (!pedidos.haCompradoLibroParaActualizar(idActor, idLibro))
                throw new ConflictoException("La compra fue cancelada o devuelta; no puede publicar otra resena.");
            Resena resena = new Resena();
            Libro libro = new Libro();
            libro.setId(idLibro);
            resena.setLibro(libro);
            resena.setUsuario(actor);
            resena.setPuntuacion(validos.puntuacion());
            resena.setComentario(validos.comentario());
            LocalDateTime ahora = LocalDateTime.now(reloj);
            resena.setFecha(ahora);
            resena.setFechaCreacion(ahora);
            resena.setUsuarioCreacion(actor.getEmail());
            resenas.insertarEnTransaccion(resena);
            TransactionContext.commit();
            return resena.getId();
        } catch (SQLException e) {
            TransactionContext.rollback();
            if ("23505".equals(e.getSQLState()) || e.getErrorCode() == 1062)
                throw new ConflictoException("Ya existe una resena publicada para este libro.");
            throw new PersistenciaException("No se pudo publicar la resena.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public DatosResenaDTO obtenerParaEditar(int idActor, int id) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        try {
            Resena resena = propia(idActor, id);
            return new DatosResenaDTO(resena.getPuntuacion(), resena.getComentario());
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar la resena propia.", e); }
    }

    @Override
    public void modificar(int idActor, int id, DatosResenaDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        DatosResenaDTO validos = validarDatos(datos);
        try {
            // Una cancelacion o devolucion posterior no impide editar una resena existente.
            Resena resena = propia(idActor, id);
            resena.setPuntuacion(validos.puntuacion());
            resena.setComentario(validos.comentario());
            resena.setUsuarioActualizacion(actor.getEmail());
            resena.setFechaActualizacion(LocalDateTime.now(reloj));
            resenas.modificar(resena);
        } catch (SQLException e) { throw new PersistenciaException("No se pudo modificar la resena.", e); }
    }

    @Override
    public void eliminarPropia(int idActor, int id) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        eliminar(id, actor, false);
    }

    @Override
    public void eliminarAdministracion(int idActor, int id) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        eliminar(id, actor, true);
    }

    private void eliminar(int id, Usuario actor, boolean administrador) throws NegocioException {
        try {
            Resena resena = resenas.leerParaActualizar(id);
            if (resena == null || resena.isEliminado()) throw new RecursoNoEncontradoException("La resena no esta disponible.");
            if (!administrador) autorizacion.propietario(actor.getId(), resena.getUsuario().getId());
            if (!resenas.eliminar(id, actor.getEmail())) throw new ConflictoException("La resena ya fue retirada.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo retirar la resena.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    private Resena propia(int idActor, int id) throws SQLException, NegocioException {
        Resena resena = resenas.leer(id);
        if (resena == null || resena.isEliminado()) throw new RecursoNoEncontradoException("La resena no esta disponible.");
        autorizacion.propietario(idActor, resena.getUsuario().getId());
        if (!resena.isActivo()) throw new ConflictoException("La resena no esta publicada.");
        return resena;
    }

    @Override
    public ResultadoPaginadoDTO<ResenaDTO> listarPorLibro(int idActor, int idLibro, ConsultaResenasDTO consulta) throws NegocioException {
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        ConsultaResenasDTO valida = validarConsulta(consulta);
        if (valida.orden() != OrdenResena.MAS_RECIENTES)
            throw new ValidacionException("orden", "Las resenas del libro se consultan por fecha reciente.");
        try {
            Libro libro = libros.leer(idLibro);
            if (libro == null || !libro.isActivo()) throw new RecursoNoEncontradoException("El libro no esta disponible.");
            return resenas.listar(idLibro, 0, idActor, valida);
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar las resenas del libro.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<ResenaDTO> listarPropias(int idActor, ConsultaResenasDTO consulta) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        ConsultaResenasDTO valida = validarConsulta(consulta);
        try { return resenas.listar(0, idActor, idActor, valida); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar sus resenas.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<ResenaDTO> listarAdministracion(int idActor, ConsultaResenasDTO consulta) throws NegocioException {
        autorizacion.administrador(idActor);
        ConsultaResenasDTO valida = validarConsulta(consulta);
        try { return resenas.listar(0, 0, idActor, valida); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar las resenas de moderacion.", e); }
    }

    @Override
    public ValoracionLibroDTO obtenerValoracion(int idActor, int idLibro) throws NegocioException {
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        try {
            Libro libro = libros.leer(idLibro);
            if (libro == null || !libro.isActivo()) throw new RecursoNoEncontradoException("El libro no esta disponible.");
            return resenas.obtenerValoracion(idLibro);
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar la valoracion del libro.", e); }
    }

    private ConsultaResenasDTO validarConsulta(ConsultaResenasDTO consulta) throws ValidacionException {
        if (consulta == null) consulta = new ConsultaResenasDTO();
        Validaciones.pagina(consulta.pagina(), consulta.tamanioPagina());
        if (consulta.orden() == null) throw new ValidacionException("orden", "Seleccione un orden de resenas.");
        return consulta;
    }

    private DatosResenaDTO validarDatos(DatosResenaDTO datos) throws ValidacionException {
        if (datos == null) throw new ValidacionException("datos", "Complete puntuacion y comentario.");
        if (datos.puntuacion() < 1 || datos.puntuacion() > 5)
            throw new ValidacionException("puntuacion", "Seleccione de una a cinco estrellas.");
        String comentario = Validaciones.texto(datos.comentario(), "comentario", 65535);
        if (comentario.getBytes(StandardCharsets.UTF_8).length > 65535)
            throw new ValidacionException("comentario", "El comentario es demasiado extenso.");
        return new DatosResenaDTO(datos.puntuacion(), comentario);
    }
}
