package me.Business.catalogo;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.CategoriaDAO;
import me.DAO.catalogo.CategoriaDAOImpl;
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
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.usuario.Usuario;

public class CategoriaBOImpl implements CategoriaBO {

    private final CategoriaDAO categoriaDAO = new CategoriaDAOImpl();
    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (registro == null)
                throw new ValidacionException("registro", "Complete los datos de la categoría.");
            DatosReferenciaDTO datos = registro.datos();
            Categoria categoria = new Categoria();
            categoria.setNombre(datos.nombre().strip());
            categoria.setDescripcion(datos.introduccion());
            categoria.setImagen(registro.imagen());
            categoria.setUsuarioCreacion(actor.getEmail());
            categoria.setFechaCreacion(LocalDateTime.now());
            if (categoriaDAO.existeNombre(datos.nombre().strip(), 0))
                throw new ValidacionException("nombre", "Ya existe una categoria con ese nombre.");
            categoriaDAO.insertar(categoria);
            return categoria.getId();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo registrar la categoria.", e);
        }
    }

    @Override
    public void modificar(int idActor, int id, DatosReferenciaDTO datos) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);

            Categoria categoria = categoriaDAO.leer(id);
            if (categoria == null || categoria.isEliminado())
                throw new RecursoNoEncontradoException("La categoria no está disponible.");
            if (categoriaDAO.existeNombre(datos.nombre().strip(), id))
                throw new ValidacionException("nombre", "Ya existe una categoria con ese nombre.");
            categoria.setNombre(datos.nombre().strip());
            categoria.setDescripcion(datos.introduccion());
            categoria.setUsuarioActualizacion(actor.getEmail());
            categoriaDAO.modificar(categoria);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo modificar la categoria.", e);
        }
    }

    @Override
    public void cambiarImagen(int idActor, int id, String imagen) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Categoria categoria = categoriaDAO.leer(id);
            if (categoria == null || categoria.isEliminado())
                throw new RecursoNoEncontradoException("La categoria no está disponible.");
            if (!categoriaDAO.cambiarImagen(id, imagen, actor.getEmail()))
                throw new NegocioException("La categoría cambio; vuelva a consultarlo.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la imagen de la categoría.", e);
        }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Categoria categoria = categoriaDAO.leer(id);
            if (categoria == null || categoria.isEliminado())
                throw new RecursoNoEncontradoException("La categoría no esta disponible.");
            if (categoria.isActivo() != activo) {
                if (!categoriaDAO.cambiarActivo(id, activo, actor.getEmail()))
                    throw new NegocioException("La categoría cambio; vuelva a consultarlo.");
                libroDAO.recalcularVetosPorCategoria(id, actor.getEmail());
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cambiar el estado de la categoría.", e);
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
            Categoria categoria = categoriaDAO.leer(id);
            if (categoria == null || categoria.isEliminado())
                throw new RecursoNoEncontradoException("La categoría no esta disponible.");
            if (!categoriaDAO.eliminar(id, actor.getEmail()))
                throw new NegocioException("La categoría cambio; vuelva a consultarlo.");
            libroDAO.recalcularVetosPorCategoria(id, actor.getEmail());
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo eliminar la categoria.", e);
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
            return categoriaDAO.buscar(filtro);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las categorias.", e);
        }
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        try {
            DetalleReferenciaDTO detalle = categoriaDAO.obtenerDetalle(id, idActor);
            if (detalle == null)
                throw new RecursoNoEncontradoException("La categoría no esta disponible.");
            return detalle;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la categoria.", e);
        }
    }

}
