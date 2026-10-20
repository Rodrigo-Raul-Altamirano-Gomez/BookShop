package me.Business.catalogo;

import java.nio.charset.StandardCharsets;
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
import me.Business.Validaciones;
import me.DAO.catalogo.CategoriaDAO;
import me.DAO.catalogo.CategoriaDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DatosReferenciaDTO;
import me.Model.dto.catalogo.DetalleReferenciaDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.RegistroReferenciaDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.usuario.Usuario;

public class CategoriaBOImpl implements CategoriaBO {
    private final CategoriaDAO dao;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public CategoriaBOImpl() { this(new CategoriaDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima"))); }

    public CategoriaBOImpl(CategoriaDAO dao, UsuarioDAO usuarios, Clock reloj) {
        this.dao = dao;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (registro == null) throw new ValidacionException("registro", "Complete los datos dla categoria.");
        DatosReferenciaDTO datos = validarDatos(registro.datos());
        Validaciones.imagen(registro.imagen());
        Categoria categoria = new Categoria();
        categoria.setNombre(datos.nombre());
        categoria.setDescripcion(datos.introduccion());
        if (registro.imagen() != null) {
            categoria.setImagen(registro.imagen().contenido());
            categoria.setTipoImagen(registro.imagen().tipoMime());
        }
        categoria.setUsuarioCreacion(actor.getEmail());
        categoria.setFechaCreacion(LocalDateTime.now(reloj));
        try {
            if (dao.existeNombre(datos.nombre(), 0)) throw new ValidacionException("nombre", "Ya existe una categoria con ese nombre.");
            dao.insertar(categoria);
            return categoria.getId();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()) || e.getErrorCode() == 1062)
                throw new ConflictoException("Ya existe una categoria con ese nombre.");
            throw new PersistenciaException("No se pudo registrar la categoria.", e);
        }
    }

    @Override
    public void modificar(int idActor, int id, DatosReferenciaDTO datos) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        DatosReferenciaDTO validos = validarDatos(datos);
        try {
            Categoria categoria = existente(id);
            if (dao.existeNombre(validos.nombre(), id)) throw new ValidacionException("nombre", "Ya existe una categoria con ese nombre.");
            categoria.setNombre(validos.nombre());
            categoria.setDescripcion(validos.introduccion());
            categoria.setUsuarioActualizacion(actor.getEmail());
            dao.modificar(categoria);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()) || e.getErrorCode() == 1062)
                throw new ConflictoException("Ya existe una categoria con ese nombre.");
            throw new PersistenciaException("No se pudo modificar la categoria.", e);
        }
    }

    @Override
    public void cambiarImagen(int idActor, int id, ImagenDTO imagen) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        Validaciones.imagen(imagen);
        try {
            existente(id);
            if (!dao.cambiarImagen(id, imagen, actor.getEmail())) throw new ConflictoException("El categoria cambio; vuelva a consultarlo.");
        } catch (SQLException e) { throw new PersistenciaException("No se pudo cambiar la imagen dla categoria.", e); }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Categoria categoria = dao.leerParaActualizar(id);
            if (categoria == null || categoria.isEliminado()) throw new RecursoNoEncontradoException("El categoria no esta disponible.");
            if (categoria.isActivo() != activo && !dao.cambiarActivo(id, activo, actor.getEmail()))
                throw new ConflictoException("El categoria cambio; vuelva a consultarlo.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cambiar el estado dla categoria.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public void eliminar(int idActor, int id) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Categoria categoria = dao.leerParaActualizar(id);
            if (categoria == null || categoria.isEliminado()) throw new RecursoNoEncontradoException("El categoria no esta disponible.");
            if (!dao.eliminar(id, actor.getEmail())) throw new ConflictoException("El categoria cambio; vuelva a consultarlo.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo eliminar la categoria.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        FiltroReferenciaDTO valido = validarFiltro(idActor, filtro);
        try { return dao.buscar(valido); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar las categorias.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscarOpciones(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        FiltroReferenciaDTO valido = validarFiltro(idActor, filtro);
        try { return dao.buscarOpciones(valido); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar las opciones de categorias.", e); }
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        try {
            DetalleReferenciaDTO detalle = dao.obtenerDetalle(id, idActor);
            if (detalle == null) throw new RecursoNoEncontradoException("El categoria no esta disponible.");
            if (!detalle.activo()) autorizacion.administrador(idActor);
            return detalle;
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar la categoria.", e); }
    }

    private Categoria existente(int id) throws SQLException, NegocioException {
        Categoria categoria = dao.leer(id);
        if (categoria == null || categoria.isEliminado()) throw new RecursoNoEncontradoException("El categoria no esta disponible.");
        return categoria;
    }

    private DatosReferenciaDTO validarDatos(DatosReferenciaDTO datos) throws ValidacionException {
        if (datos == null) throw new ValidacionException("datos", "Complete los datos dla categoria.");
        String nombre = Validaciones.texto(datos.nombre(), "nombre", 100);
        String introduccion = Validaciones.opcional(datos.introduccion(), "introduccion", 65535);
        if (introduccion != null && introduccion.getBytes(StandardCharsets.UTF_8).length > 65535)
            throw new ValidacionException("introduccion", "La introduccion es demasiado extensa.");
        return new DatosReferenciaDTO(nombre, introduccion);
    }

    private FiltroReferenciaDTO validarFiltro(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        if (filtro == null) filtro = new FiltroReferenciaDTO();
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        if (!filtro.soloActivos()) autorizacion.administrador(idActor);
        Validaciones.pagina(filtro.pagina(), filtro.tamanioPagina());
        if (filtro.orden() == null) throw new ValidacionException("orden", "Seleccione un orden.");
        return new FiltroReferenciaDTO(Validaciones.opcional(filtro.nombre(), "nombre", 100), filtro.soloActivos(),
                filtro.orden(), filtro.pagina(), filtro.tamanioPagina());
    }
}




