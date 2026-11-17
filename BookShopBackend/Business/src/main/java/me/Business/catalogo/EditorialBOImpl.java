package me.Business.catalogo;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.EditorialDAO;
import me.DAO.catalogo.EditorialDAOImpl;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DatosReferenciaDTO;
import me.Model.dto.catalogo.DetalleReferenciaDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.RegistroReferenciaDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.usuario.Usuario;

public class EditorialBOImpl implements EditorialBO {

    private final EditorialDAO editorialDAO = new EditorialDAOImpl();
    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (registro == null)
                throw new ValidacionException("registro", "Complete los datos de la editorial.");
            DatosReferenciaDTO datos = registro.datos();
            Editorial editorial = new Editorial();
            editorial.setNombre(datos.nombre().strip());
            editorial.setDescripcion(datos.introduccion());
            editorial.setImagen(registro.imagen());
            editorial.setUsuarioCreacion(actor.getEmail());
            editorial.setFechaCreacion(LocalDateTime.now());
            if (editorialDAO.existeNombre(datos.nombre().strip(), 0))
                throw new ValidacionException("nombre", "Ya existe una editorial con ese nombre.");
            editorialDAO.insertar(editorial);
            return editorial.getId();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo registrar la editorial.", e);
        }
    }

    @Override
    public void modificar(int idActor, int id, DatosReferenciaDTO datos) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);

            Editorial editorial = editorialDAO.leer(id);
            if (editorial == null || editorial.isEliminado())
                throw new RecursoNoEncontradoException("La editorial no está disponible.");
            if (editorialDAO.existeNombre(datos.nombre().strip(), id))
                throw new ValidacionException("nombre", "Ya existe una editorial con ese nombre.");
            editorial.setNombre(datos.nombre().strip());
            editorial.setDescripcion(datos.introduccion());
            editorial.setUsuarioActualizacion(actor.getEmail());
            editorialDAO.modificar(editorial);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo modificar la editorial.", e);
        }
    }

    @Override
    public void cambiarImagen(int idActor, int id, String imagen) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Editorial editorial = editorialDAO.leer(id);
            if (editorial == null || editorial.isEliminado())
                throw new RecursoNoEncontradoException("La editorial no está disponible.");
            if (!editorialDAO.cambiarImagen(id, imagen, actor.getEmail()))
                throw new NegocioException("La editorial cambio; vuelva a consultarlo.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la imagen de la editorial.", e);
        }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Editorial editorial = editorialDAO.leer(id);
            if (editorial == null || editorial.isEliminado())
                throw new RecursoNoEncontradoException("La editorial no esta disponible.");
            if (editorial.isActivo() != activo) {
                if (!editorialDAO.cambiarActivo(id, activo, actor.getEmail()))
                    throw new NegocioException("La editorial cambio; vuelva a consultarlo.");
                libroDAO.recalcularVetosPorEditorial(id, actor.getEmail());
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cambiar el estado de la editorial.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public void eliminar(int idActor, int id) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Editorial editorial = editorialDAO.leer(id);
            if (editorial == null || editorial.isEliminado())
                throw new RecursoNoEncontradoException("La editorial no esta disponible.");
            if (!editorialDAO.eliminar(id, actor.getEmail()))
                throw new NegocioException("La editorial cambio; vuelva a consultarlo.");
            libroDAO.recalcularVetosPorEditorial(id, actor.getEmail());
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo eliminar la editorial.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(FiltroReferenciaDTO filtro) throws
        NegocioException {
        if (filtro == null)
            filtro = FiltroReferenciaDTO.porDefecto();
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        if (filtro.orden() == null)
            throw new ValidacionException("orden", "Seleccione un orden.");
        try {
            return editorialDAO.buscar(filtro);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las editoriales.", e);
        }
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        try {
            DetalleReferenciaDTO detalle = editorialDAO.obtenerDetalle(id, idActor);
            if (detalle == null)
                throw new RecursoNoEncontradoException("La editorial no esta disponible.");
            return detalle;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la editorial.", e);
        }
    }

}
