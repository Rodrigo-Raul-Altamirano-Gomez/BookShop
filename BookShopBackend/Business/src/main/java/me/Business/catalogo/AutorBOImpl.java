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
import me.DAO.catalogo.AutorDAO;
import me.DAO.catalogo.AutorDAOImpl;
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
import me.Model.entities.catalogo.Autor;
import me.Model.entities.usuario.Usuario;

public class AutorBOImpl implements AutorBO {
    private final AutorDAO dao;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public AutorBOImpl() { this(new AutorDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima"))); }

    public AutorBOImpl(AutorDAO dao, UsuarioDAO usuarios, Clock reloj) {
        this.dao = dao;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (registro == null) throw new ValidacionException("registro", "Complete los datos del autor.");
        DatosReferenciaDTO datos = validarDatos(registro.datos());
        Validaciones.imagen(registro.imagen());
        Autor autor = new Autor();
        autor.setNombre(datos.nombre());
        autor.setBiografia(datos.introduccion());
        if (registro.imagen() != null) {
            autor.setImagen(registro.imagen().contenido());
            autor.setTipoImagen(registro.imagen().tipoMime());
        }
        autor.setUsuarioCreacion(actor.getEmail());
        autor.setFechaCreacion(LocalDateTime.now(reloj));
        try {
            dao.insertar(autor);
            return autor.getId();
        } catch (SQLException e) { throw new PersistenciaException("No se pudo registrar el autor.", e); }
    }

    @Override
    public void modificar(int idActor, int id, DatosReferenciaDTO datos) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        DatosReferenciaDTO validos = validarDatos(datos);
        try {
            Autor autor = existente(id);
            autor.setNombre(validos.nombre());
            autor.setBiografia(validos.introduccion());
            autor.setUsuarioActualizacion(actor.getEmail());
            dao.modificar(autor);
        } catch (SQLException e) { throw new PersistenciaException("No se pudo modificar el autor.", e); }
    }

    @Override
    public void cambiarImagen(int idActor, int id, ImagenDTO imagen) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        Validaciones.imagen(imagen);
        try {
            existente(id);
            if (!dao.cambiarImagen(id, imagen, actor.getEmail())) throw new ConflictoException("El autor cambio; vuelva a consultarlo.");
        } catch (SQLException e) { throw new PersistenciaException("No se pudo cambiar la imagen del autor.", e); }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Autor autor = dao.leerParaActualizar(id);
            if (autor == null || autor.isEliminado()) throw new RecursoNoEncontradoException("El autor no esta disponible.");
            if (autor.isActivo() != activo && !dao.cambiarActivo(id, activo, actor.getEmail()))
                throw new ConflictoException("El autor cambio; vuelva a consultarlo.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cambiar el estado del autor.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public void eliminar(int idActor, int id) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Autor autor = dao.leerParaActualizar(id);
            if (autor == null || autor.isEliminado()) throw new RecursoNoEncontradoException("El autor no esta disponible.");
            if (!dao.eliminar(id, actor.getEmail())) throw new ConflictoException("El autor cambio; vuelva a consultarlo.");
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo eliminar el autor.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        FiltroReferenciaDTO valido = validarFiltro(idActor, filtro);
        try { return dao.buscar(valido); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los autores.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscarOpciones(int idActor, FiltroReferenciaDTO filtro) throws NegocioException {
        FiltroReferenciaDTO valido = validarFiltro(idActor, filtro);
        try { return dao.buscarOpciones(valido); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar las opciones de autores.", e); }
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        try {
            DetalleReferenciaDTO detalle = dao.obtenerDetalle(id, idActor);
            if (detalle == null) throw new RecursoNoEncontradoException("El autor no esta disponible.");
            if (!detalle.activo()) autorizacion.administrador(idActor);
            return detalle;
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar el autor.", e); }
    }

    private Autor existente(int id) throws SQLException, NegocioException {
        Autor autor = dao.leer(id);
        if (autor == null || autor.isEliminado()) throw new RecursoNoEncontradoException("El autor no esta disponible.");
        return autor;
    }

    private DatosReferenciaDTO validarDatos(DatosReferenciaDTO datos) throws ValidacionException {
        if (datos == null) throw new ValidacionException("datos", "Complete los datos del autor.");
        String nombre = Validaciones.texto(datos.nombre(), "nombre", 200);
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
        return new FiltroReferenciaDTO(Validaciones.opcional(filtro.nombre(), "nombre", 200), filtro.soloActivos(),
                filtro.orden(), filtro.pagina(), filtro.tamanioPagina());
    }
}

