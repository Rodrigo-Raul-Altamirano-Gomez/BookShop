package me.Business.catalogo;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.Business.Imagenes;
import me.DAO.catalogo.CategoriaDAO;
import me.DAO.catalogo.CategoriaDAOImpl;
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
    private final CategoriaDAO categoriaDAO;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public CategoriaBOImpl() { this(new CategoriaDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima"))); }

    public CategoriaBOImpl(CategoriaDAO categoriaDAO, UsuarioDAO usuarioDAO, Clock reloj) {
        this.categoriaDAO = categoriaDAO;
        this.autorizacion = new Autorizacion(usuarioDAO);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (registro == null) throw new ValidacionException("registro", "Complete los datos de la categoría.");
        DatosReferenciaDTO datos = validarFormulario(registro.datos());
        Imagenes.validar(registro.imagen());
        Categoria categoria = new Categoria();
        categoria.setNombre(datos.nombre());
        categoria.setDescripcion(datos.introduccion());
        categoria.setImagen(registro.imagen());
        categoria.setUsuarioCreacion(actor.getEmail());
        categoria.setFechaCreacion(LocalDateTime.now(reloj));
        try {
            if (categoriaDAO.existeNombre(datos.nombre(), 0)) throw new ValidacionException("nombre", "Ya existe una categoria con ese nombre.");
            categoriaDAO.insertar(categoria);
            return categoria.getId();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo registrar la categoria.", e);
        }
    }

    @Override
    public void modificar(int idActor, int id, DatosReferenciaDTO datos) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        DatosReferenciaDTO validos = validarFormulario(datos);
        try {
            Categoria categoria = categoriaDAO.leer(id);
            if (categoria == null || categoria.isEliminado())
                throw new RecursoNoEncontradoException("La categoria no está disponible.");
            if (categoriaDAO.existeNombre(validos.nombre(), id)) throw new ValidacionException("nombre", "Ya existe una categoria con ese nombre.");
            categoria.setNombre(validos.nombre());
            categoria.setDescripcion(validos.introduccion());
            categoria.setUsuarioActualizacion(actor.getEmail());
            categoriaDAO.modificar(categoria);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo modificar la categoria.", e);
        }
    }

    @Override
    public void cambiarImagen(int idActor, int id, String imagen) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        Imagenes.validar(imagen);
        try {
            Categoria categoria = categoriaDAO.leer(id);
            if (categoria == null || categoria.isEliminado())
                throw new RecursoNoEncontradoException("La categoria no está disponible.");
            if (!categoriaDAO.cambiarImagen(id, imagen, actor.getEmail())) throw new ConflictoException("La categoría cambio; vuelva a consultarlo.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la imagen de la categoría.", e);
        }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Categoria categoria = categoriaDAO.leerParaActualizar(id);
            if (categoria == null || categoria.isEliminado()) throw new RecursoNoEncontradoException("La categoría no esta disponible.");
            if (categoria.isActivo() != activo && !categoriaDAO.cambiarActivo(id, activo, actor.getEmail()))
                throw new ConflictoException("La categoría cambio; vuelva a consultarlo.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cambiar el estado de la categoría.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        }
        finally {
            TransactionContext.close();
        }
    }

    @Override
    public void eliminar(int idActor, int id) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Categoria categoria = categoriaDAO.leerParaActualizar(id);
            if (categoria == null || categoria.isEliminado()) throw new RecursoNoEncontradoException("La categoría no esta disponible.");
            if (!categoriaDAO.eliminar(id, actor.getEmail())) throw new ConflictoException("La categoría cambio; vuelva a consultarlo.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo eliminar la categoria.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        }
        finally {
            TransactionContext.close();
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        FiltroReferenciaDTO valido = validarConsulta(idActor, filtro);
        try {
            return categoriaDAO.buscar(valido);
        }
        catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las categorias.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscarOpciones(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        FiltroReferenciaDTO valido = validarConsulta(idActor, filtro);
        try {
            return categoriaDAO.buscarOpciones(valido);
        }
        catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las opciones de categorias.", e);
        }
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        try {
            DetalleReferenciaDTO detalle = categoriaDAO.obtenerDetalle(id, idActor);
            if (detalle == null) throw new RecursoNoEncontradoException("La categoría no esta disponible.");
            if (!detalle.activo()) autorizacion.administrador(idActor);
            return detalle;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la categoria.", e);
        }
    }

    private DatosReferenciaDTO validarFormulario(DatosReferenciaDTO datos) throws ValidacionException {
        if (datos == null) throw new ValidacionException("datos", "Complete los datos de la categoría.");
        String nombre = datos.nombre() == null ? "" : datos.nombre().strip();
        if (nombre.isEmpty()) throw new ValidacionException("nombre", "Este campo es obligatorio.");
        if (nombre.length() > 100)
            throw new ValidacionException("nombre", "El máximo es 100 caracteres.");
        String introduccion = datos.introduccion() == null ? null : datos.introduccion().strip();
        if (introduccion != null && introduccion.isEmpty()) introduccion = null;
        if (introduccion != null && introduccion.length() > 65535)
            throw new ValidacionException("introduccion", "El máximo es 65535 caracteres.");
        return new DatosReferenciaDTO(nombre, introduccion);
    }

    private FiltroReferenciaDTO validarConsulta(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        if (filtro == null) filtro = FiltroReferenciaDTO.porDefecto();
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        if (!filtro.soloActivos()) autorizacion.administrador(idActor);
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        if (filtro.orden() == null) throw new ValidacionException("orden", "Seleccione un orden.");
        String nombre = filtro.nombre() == null ? null : filtro.nombre().strip();
        if (nombre != null && nombre.isEmpty()) nombre = null;
        if (nombre != null && nombre.length() > 100)
            throw new ValidacionException("nombre", "El máximo es 100 caracteres.");
        return new FiltroReferenciaDTO(nombre, filtro.soloActivos(),
                filtro.orden(), filtro.pagina(), filtro.tamanioPagina());
    }
}
