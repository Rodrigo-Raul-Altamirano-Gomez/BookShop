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
import me.DAO.catalogo.EditorialDAO;
import me.DAO.catalogo.EditorialDAOImpl;
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

    private final EditorialDAO editorialDAO;
    private final Clock reloj;

    public EditorialBOImpl() {
        this(new EditorialDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public EditorialBOImpl(EditorialDAO editorialDAO, UsuarioDAO usuarioDAO, Clock reloj) {
        this.editorialDAO = editorialDAO;
        this.usuarioDAO = usuarioDAO;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (registro == null)
                throw new ValidacionException("registro", "Complete los datos de la editorial.");
            DatosReferenciaDTO datos = validarFormulario(registro.datos());
            if (registro.imagen() != null && registro.imagen().length() > 2048)
                throw new ValidacionException("imagen", "La URL admite hasta 2048 caracteres.");
            Editorial editorial = new Editorial();
            editorial.setNombre(datos.nombre());
            editorial.setDescripcion(datos.introduccion());
            editorial.setImagen(registro.imagen());
            editorial.setUsuarioCreacion(actor.getEmail());
            editorial.setFechaCreacion(LocalDateTime.now(reloj));
            if (editorialDAO.existeNombre(datos.nombre(), 0))
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
            DatosReferenciaDTO validos = validarFormulario(datos);
            Editorial editorial = editorialDAO.leer(id);
            if (editorial == null || editorial.isEliminado())
                throw new RecursoNoEncontradoException("La editorial no está disponible.");
            if (editorialDAO.existeNombre(validos.nombre(), id))
                throw new ValidacionException("nombre", "Ya existe una editorial con ese nombre.");
            editorial.setNombre(validos.nombre());
            editorial.setDescripcion(validos.introduccion());
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
            if (imagen != null && imagen.length() > 2048)
                throw new ValidacionException("imagen", "La URL admite hasta 2048 caracteres.");
            Editorial editorial = editorialDAO.leer(id);
            if (editorial == null || editorial.isEliminado())
                throw new RecursoNoEncontradoException("La editorial no está disponible.");
            if (!editorialDAO.cambiarImagen(id, imagen, actor.getEmail()))
                throw new ConflictoException("La editorial cambio; vuelva a consultarlo.");
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
            if (editorial.isActivo() != activo && !editorialDAO.cambiarActivo(id, activo, actor.getEmail()))
                throw new ConflictoException("La editorial cambio; vuelva a consultarlo.");
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
                throw new ConflictoException("La editorial cambio; vuelva a consultarlo.");
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
        if (nombre != null && nombre.length() > 150)
            throw new ValidacionException("nombre", "El máximo es 150 caracteres.");
        FiltroReferenciaDTO valido = new FiltroReferenciaDTO(nombre, filtro.soloActivos(), filtro.orden(),
            filtro.pagina(), filtro.tamanioPagina());
        try {
            return editorialDAO.buscar(valido);
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

    private DatosReferenciaDTO validarFormulario(DatosReferenciaDTO datos) throws ValidacionException {
        if (datos == null)
            throw new ValidacionException("datos", "Complete los datos de la editorial.");
        String nombre = datos.nombre() == null ? "" : datos.nombre().strip();
        if (nombre.isEmpty())
            throw new ValidacionException("nombre", "Este campo es obligatorio.");
        if (nombre.length() > 150)
            throw new ValidacionException("nombre", "El máximo es 150 caracteres.");
        String introduccion = datos.introduccion() == null ? null : datos.introduccion().strip();
        if (introduccion != null && introduccion.isEmpty())
            introduccion = null;
        if (introduccion != null && introduccion.length() > 65535)
            throw new ValidacionException("introduccion", "El máximo es 65535 caracteres.");
        return new DatosReferenciaDTO(nombre, introduccion);
    }

    private final UsuarioDAO usuarioDAO;
}
