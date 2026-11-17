package me.Business.catalogo;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.AutorDAO;
import me.DAO.catalogo.AutorDAOImpl;
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
import me.Model.entities.catalogo.Autor;
import me.Model.entities.usuario.Usuario;

public class AutorBOImpl implements AutorBO {

    private final AutorDAO autorDAO = new AutorDAOImpl();
    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (registro == null)
                throw new ValidacionException("registro", "Complete los datos del autor.");
            DatosReferenciaDTO datos = registro.datos();
            Autor autor = new Autor();
            autor.setNombre(datos.nombre().strip());
            autor.setBiografia(datos.introduccion());
            autor.setImagen(registro.imagen());
            autor.setUsuarioCreacion(actor.getEmail());
            autor.setFechaCreacion(LocalDateTime.now());
            autorDAO.insertar(autor);
            return autor.getId();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo registrar el autor.", e);
        }
    }

    @Override
    public void modificar(int idActor, int id, DatosReferenciaDTO datos) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);

            Autor autor = autorDAO.leer(id);
            if (autor == null || autor.isEliminado())
                throw new RecursoNoEncontradoException("El autor no está disponible.");
            autor.setNombre(datos.nombre().strip());
            autor.setBiografia(datos.introduccion());
            autor.setUsuarioActualizacion(actor.getEmail());
            autorDAO.modificar(autor);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo modificar el autor.", e);
        }
    }

    @Override
    public void cambiarImagen(int idActor, int id, String imagen) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Autor autor = autorDAO.leer(id);
            if (autor == null || autor.isEliminado())
                throw new RecursoNoEncontradoException("El autor no está disponible.");
            if (!autorDAO.cambiarImagen(id, imagen, actor.getEmail()))
                throw new NegocioException("El autor cambio; vuelva a consultarlo.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la imagen del autor.", e);
        }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Autor autor = autorDAO.leer(id);
            if (autor == null || autor.isEliminado())
                throw new RecursoNoEncontradoException("El autor no esta disponible.");
            if (autor.isActivo() != activo) {
                if (!autorDAO.cambiarActivo(id, activo, actor.getEmail()))
                    throw new NegocioException("El autor cambio; vuelva a consultarlo.");
                libroDAO.recalcularVetosPorAutor(id, actor.getEmail());
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cambiar el estado del autor.", e);
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
            Autor autor = autorDAO.leer(id);
            if (autor == null || autor.isEliminado())
                throw new RecursoNoEncontradoException("El autor no esta disponible.");
            if (!autorDAO.eliminar(id, actor.getEmail()))
                throw new NegocioException("El autor cambio; vuelva a consultarlo.");
            libroDAO.recalcularVetosPorAutor(id, actor.getEmail());
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo eliminar el autor.", e);
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
            return autorDAO.buscar(filtro);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los autores.", e);
        }
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        try {
            DetalleReferenciaDTO detalle = autorDAO.obtenerDetalle(id, idActor);
            if (detalle == null)
                throw new RecursoNoEncontradoException("El autor no esta disponible.");
            return detalle;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el autor.", e);
        }
    }

}
