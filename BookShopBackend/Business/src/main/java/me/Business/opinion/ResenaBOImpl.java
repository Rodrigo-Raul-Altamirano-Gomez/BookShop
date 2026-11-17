package me.Business.opinion;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
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

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final ResenaDAO resenaDAO = new ResenaDAOImpl();
    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final PedidoDAO pedidoDAO = new PedidoDAOImpl();

    @Override
    public void verificarPuedePublicar(int idActor, int idLibro) throws NegocioException {
        try {
            Libro libro = libroDAO.leer(idLibro);
            if (libro == null || !libro.isActivo())
                throw new RecursoNoEncontradoException("El libro no esta disponible.");
            if (resenaDAO.existePublicada(idActor, idLibro))
                throw new NegocioException("Ya tiene una resena publicada para este libro.");
            if (!pedidoDAO.haCompradoLibro(idActor, idLibro))
                throw new AccesoDenegadoException("Necesita una compra confirmada que no este cancelada ni" +
                    " devuelta.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar si puede publicar una resena.", e);
        }
    }

    @Override
    public int publicar(int idActor, int idLibro, DatosResenaDTO datos) throws NegocioException {
        try {
            DatosResenaDTO validos = validarDatos(datos);
            verificarPuedePublicar(idActor, idLibro);
            Usuario actor = usuarioDAO.leer(idActor);
            Libro libro = new Libro();
            libro.setId(idLibro);
            Resena resena = new Resena();
            resena.setLibro(libro);
            resena.setUsuario(actor);
            resena.setPuntuacion(validos.puntuacion());
            resena.setComentario(validos.comentario());
            LocalDateTime ahora = LocalDateTime.now();
            resena.setFecha(ahora);
            resena.setFechaCreacion(ahora);
            resena.setUsuarioCreacion(actor.getEmail());
            resenaDAO.insertar(resena);
            return resena.getId();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo publicar la resena.", e);
        }
    }

    @Override
    public void modificar(int idActor, int id, DatosResenaDTO datos) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            DatosResenaDTO validos = validarDatos(datos);
            // Una cancelacion o devolucion posterior no impide editar una resena existente.
            Resena resena = resenaDAO.leer(id);
            if (resena == null || resena.isEliminado())
                throw new RecursoNoEncontradoException("La resena no esta disponible.");
            if (idActor != resena.getUsuario().getId())
                throw new AccesoDenegadoException("La reseña pertenece a otro usuario.");
            if (!resena.isActivo())
                throw new NegocioException("La resena no esta publicada.");
            resena.setPuntuacion(validos.puntuacion());
            resena.setComentario(validos.comentario());
            resena.setUsuarioActualizacion(actor.getEmail());
            resena.setFechaActualizacion(LocalDateTime.now());
            resenaDAO.modificar(resena);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo modificar la resena.", e);
        }
    }

    @Override
    public void eliminarPropia(int idActor, int id) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            eliminar(id, actor, false);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la cuenta.", e);
        }
    }

    @Override
    public void eliminarAdministracion(int idActor, int id) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            eliminar(id, actor, true);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la cuenta.", e);
        }
    }

    private void eliminar(int id, Usuario actor, boolean administrador) throws NegocioException {
        try {
            Resena resena = resenaDAO.leer(id);
            if (resena == null || resena.isEliminado())
                throw new RecursoNoEncontradoException("La resena no esta disponible.");
            if (!administrador && actor.getId() != resena.getUsuario().getId())
                throw new AccesoDenegadoException("La reseña pertenece a otro usuario.");
            if (!resenaDAO.eliminar(id, actor.getEmail()))
                throw new NegocioException("La resena ya fue retirada.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo retirar la resena.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public ResultadoPaginadoDTO<ResenaDTO> listarPorLibro(int idActor, int idLibro, ConsultaResenasDTO
        consulta) throws NegocioException {
        if (consulta == null)
            consulta = ConsultaResenasDTO.porDefecto();
        if (consulta.pagina() < 1 || consulta.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        if (consulta.orden() != OrdenResena.MAS_RECIENTES)
            throw new ValidacionException("orden", "Las resenas del libro se consultan por fecha reciente.");
        try {
            Libro libro = libroDAO.leer(idLibro);
            if (libro == null || !libro.isActivo())
                throw new RecursoNoEncontradoException("El libro no esta disponible.");
            return resenaDAO.listar(idLibro, 0, idActor, consulta);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las resenas del libro.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<ResenaDTO> listarPropias(int idActor, ConsultaResenasDTO consulta) throws
        NegocioException {
        if (consulta == null)
            consulta = ConsultaResenasDTO.porDefecto();
        if (consulta.pagina() < 1 || consulta.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        if (consulta.orden() == null)
            throw new ValidacionException("orden", "Seleccione un orden de resenas.");
        try {
            return resenaDAO.listar(0, idActor, idActor, consulta);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar sus resenas.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<ResenaDTO> listarAdministracion(int idActor, ConsultaResenasDTO consulta)
        throws NegocioException {
        if (consulta == null)
            consulta = ConsultaResenasDTO.porDefecto();
        if (consulta.pagina() < 1 || consulta.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        if (consulta.orden() == null)
            throw new ValidacionException("orden", "Seleccione un orden de resenas.");
        try {
            return resenaDAO.listar(0, 0, idActor, consulta);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las resenas de moderacion.", e);
        }
    }

    @Override
    public ValoracionLibroDTO obtenerValoracion(int idLibro) throws NegocioException {
        try {
            Libro libro = libroDAO.leer(idLibro);
            if (libro == null || !libro.isActivo())
                throw new RecursoNoEncontradoException("El libro no esta disponible.");
            return resenaDAO.obtenerValoracion(idLibro);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la valoracion del libro.", e);
        }
    }

    private DatosResenaDTO validarDatos(DatosResenaDTO datos) throws ValidacionException {
        if (datos == null)
            throw new ValidacionException("datos", "Complete puntuacion y comentario.");
        if (datos.puntuacion() < 1 || datos.puntuacion() > 5)
            throw new ValidacionException("puntuacion", "Seleccione de una a cinco estrellas.");
        String comentario = datos.comentario() == null ? "" : datos.comentario().strip();
        if (comentario.isEmpty())
            throw new ValidacionException("comentario", "Este campo es obligatorio.");
        return new DatosResenaDTO(datos.puntuacion(), comentario);
    }

}
