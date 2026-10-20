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
import me.DAO.catalogo.EditorialDAO;
import me.DAO.catalogo.EditorialDAOImpl;
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
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.usuario.Usuario;

public class EditorialBOImpl implements EditorialBO {
    private final EditorialDAO dao;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public EditorialBOImpl() { this(new EditorialDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima"))); }

    public EditorialBOImpl(EditorialDAO dao, UsuarioDAO usuarios, Clock reloj) {
        this.dao = dao;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (registro == null) throw new ValidacionException("registro", "Complete los datos dla editorial.");
        DatosReferenciaDTO datos = validarDatos(registro.datos());
        Validaciones.imagen(registro.imagen());
        Editorial editorial = new Editorial();
        editorial.setNombre(datos.nombre());
        editorial.setDescripcion(datos.introduccion());
        if (registro.imagen() != null) {
            editorial.setImagen(registro.imagen().contenido());
            editorial.setTipoImagen(registro.imagen().tipoMime());
        }
        editorial.setUsuarioCreacion(actor.getEmail());
        editorial.setFechaCreacion(LocalDateTime.now(reloj));
        try {
            if (dao.existeNombre(datos.nombre(), 0)) throw new ValidacionException("nombre", "Ya existe una editorial con ese nombre.");
            dao.insertar(editorial);
            return editorial.getId();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()) || e.getErrorCode() == 1062)
                throw new ConflictoException("Ya existe una editorial con ese nombre.");
            throw new PersistenciaException("No se pudo registrar la editorial.", e);
        }
    }

    @Override
    public void modificar(int idActor, int id, DatosReferenciaDTO datos) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        DatosReferenciaDTO validos = validarDatos(datos);
        try {
            Editorial editorial = existente(id);
            if (dao.existeNombre(validos.nombre(), id)) throw new ValidacionException("nombre", "Ya existe una editorial con ese nombre.");
            editorial.setNombre(validos.nombre());
            editorial.setDescripcion(validos.introduccion());
            editorial.setUsuarioActualizacion(actor.getEmail());
            dao.modificar(editorial);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()) || e.getErrorCode() == 1062)
                throw new ConflictoException("Ya existe una editorial con ese nombre.");
            throw new PersistenciaException("No se pudo modificar la editorial.", e);
        }
    }

    @Override
    public void cambiarImagen(int idActor, int id, ImagenDTO imagen) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        Validaciones.imagen(imagen);
        try {
            existente(id);
            if (!dao.cambiarImagen(id, imagen, actor.getEmail())) throw new ConflictoException("El editorial cambio; vuelva a consultarlo.");
        } catch (SQLException e) { throw new PersistenciaException("No se pudo cambiar la imagen dla editorial.", e); }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Editorial editorial = dao.leerParaActualizar(id);
            if (editorial == null || editorial.isEliminado()) throw new RecursoNoEncontradoException("El editorial no esta disponible.");
            if (editorial.isActivo() != activo && !dao.cambiarActivo(id, activo, actor.getEmail()))
                throw new ConflictoException("El editorial cambio; vuelva a consultarlo.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cambiar el estado dla editorial.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public void eliminar(int idActor, int id) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Editorial editorial = dao.leerParaActualizar(id);
            if (editorial == null || editorial.isEliminado()) throw new RecursoNoEncontradoException("El editorial no esta disponible.");
            if (!dao.eliminar(id, actor.getEmail())) throw new ConflictoException("El editorial cambio; vuelva a consultarlo.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo eliminar la editorial.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        FiltroReferenciaDTO valido = validarFiltro(idActor, filtro);
        try { return dao.buscar(valido); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar las editoriales.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscarOpciones(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        FiltroReferenciaDTO valido = validarFiltro(idActor, filtro);
        try { return dao.buscarOpciones(valido); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar las opciones de editoriales.", e); }
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        try {
            DetalleReferenciaDTO detalle = dao.obtenerDetalle(id, idActor);
            if (detalle == null) throw new RecursoNoEncontradoException("El editorial no esta disponible.");
            if (!detalle.activo()) autorizacion.administrador(idActor);
            return detalle;
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar la editorial.", e); }
    }

    private Editorial existente(int id) throws SQLException, NegocioException {
        Editorial editorial = dao.leer(id);
        if (editorial == null || editorial.isEliminado()) throw new RecursoNoEncontradoException("El editorial no esta disponible.");
        return editorial;
    }

    private DatosReferenciaDTO validarDatos(DatosReferenciaDTO datos) throws ValidacionException {
        if (datos == null) throw new ValidacionException("datos", "Complete los datos dla editorial.");
        String nombre = Validaciones.texto(datos.nombre(), "nombre", 150);
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
        return new FiltroReferenciaDTO(Validaciones.opcional(filtro.nombre(), "nombre", 150), filtro.soloActivos(),
                filtro.orden(), filtro.pagina(), filtro.tamanioPagina());
    }
}




