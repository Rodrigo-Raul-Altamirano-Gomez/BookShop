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
import me.DAO.catalogo.AutorDAO;
import me.DAO.catalogo.AutorDAOImpl;
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

    private final AutorDAO autorDAO;
    private final Clock reloj;

    public AutorBOImpl() {
        this(new AutorDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public AutorBOImpl(AutorDAO autorDAO, UsuarioDAO usuarioDAO, Clock reloj) {
        this.autorDAO = autorDAO;
        this.usuarioDAO = usuarioDAO;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (registro == null)
                throw new ValidacionException("registro", "Complete los datos del autor.");
            DatosReferenciaDTO datos = validarFormulario(registro.datos());
            if (registro.imagen() != null && registro.imagen().length() > 2048)
                throw new ValidacionException("imagen", "La URL admite hasta 2048 caracteres.");
            Autor autor = new Autor();
            autor.setNombre(datos.nombre());
            autor.setBiografia(datos.introduccion());
            autor.setImagen(registro.imagen());
            autor.setUsuarioCreacion(actor.getEmail());
            autor.setFechaCreacion(LocalDateTime.now(reloj));
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
            DatosReferenciaDTO validos = validarFormulario(datos);
            Autor autor = autorDAO.leer(id);
            if (autor == null || autor.isEliminado())
                throw new RecursoNoEncontradoException("El autor no está disponible.");
            autor.setNombre(validos.nombre());
            autor.setBiografia(validos.introduccion());
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
            if (imagen != null && imagen.length() > 2048)
                throw new ValidacionException("imagen", "La URL admite hasta 2048 caracteres.");
            Autor autor = autorDAO.leer(id);
            if (autor == null || autor.isEliminado())
                throw new RecursoNoEncontradoException("El autor no está disponible.");
            if (!autorDAO.cambiarImagen(id, imagen, actor.getEmail()))
                throw new ConflictoException("El autor cambio; vuelva a consultarlo.");
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
            if (autor.isActivo() != activo && !autorDAO.cambiarActivo(id, activo, actor.getEmail()))
                throw new ConflictoException("El autor cambio; vuelva a consultarlo.");
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
                throw new ConflictoException("El autor cambio; vuelva a consultarlo.");
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
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(int idActor, FiltroReferenciaDTO filtro) throws
        NegocioException {
        if (filtro == null)
            filtro = FiltroReferenciaDTO.porDefecto();
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        if (filtro.orden() == null)
            throw new ValidacionException("orden", "Seleccione un orden.");
        String nombre = filtro.nombre() == null ? null : filtro.nombre().strip();
        if (nombre != null && nombre.isEmpty())
            nombre = null;
        if (nombre != null && nombre.length() > 200)
            throw new ValidacionException("nombre", "El máximo es 200 caracteres.");
        FiltroReferenciaDTO valido = new FiltroReferenciaDTO(nombre, filtro.soloActivos(), filtro.orden(),
            filtro.pagina(), filtro.tamanioPagina());
        try {
            return autorDAO.buscar(valido);
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

    private DatosReferenciaDTO validarFormulario(DatosReferenciaDTO datos) throws ValidacionException {
        if (datos == null)
            throw new ValidacionException("datos", "Complete los datos del autor.");
        String nombre = datos.nombre() == null ? "" : datos.nombre().strip();
        if (nombre.isEmpty())
            throw new ValidacionException("nombre", "Este campo es obligatorio.");
        if (nombre.length() > 200)
            throw new ValidacionException("nombre", "El máximo es 200 caracteres.");
        String introduccion = datos.introduccion() == null ? null : datos.introduccion().strip();
        if (introduccion != null && introduccion.isEmpty())
            introduccion = null;
        if (introduccion != null && introduccion.length() > 65535)
            throw new ValidacionException("introduccion", "El máximo es 65535 caracteres.");
        return new DatosReferenciaDTO(nombre, introduccion);
    }

    private final UsuarioDAO usuarioDAO;
}
