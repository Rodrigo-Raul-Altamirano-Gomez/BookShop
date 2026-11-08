package me.Business.usuario;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.DAO.usuario.DireccionEnvioDAO;
import me.DAO.usuario.DireccionEnvioDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DatosDireccionEnvioDTO;
import me.Model.dto.compra.DireccionEnvioDTO;
import me.Model.entities.compra.DireccionEnvio;
import me.Model.entities.usuario.Usuario;

public class DireccionEnvioBOImpl implements DireccionEnvioBO {
    private final DireccionEnvioDAO direccionEnvioDAO;
    private final UsuarioDAO usuarioDAO;
    private final Clock reloj;
    private final Autorizacion autorizacion;

    public DireccionEnvioBOImpl() {
        this(new DireccionEnvioDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public DireccionEnvioBOImpl(DireccionEnvioDAO direccionEnvioDAO, UsuarioDAO usuarioDAO, Clock reloj) {
        this.direccionEnvioDAO = direccionEnvioDAO;
        this.usuarioDAO = usuarioDAO;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
        this.autorizacion = new Autorizacion(usuarioDAO);
    }

    @Override
    public DireccionEnvioDTO registrar(int idActor, DatosDireccionEnvioDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        DatosDireccionEnvioDTO validos = validarFormulario(datos);
        DireccionEnvio direccion = new DireccionEnvio(0, actor, validos.alias(), validos.calle(),
                validos.distrito(), validos.ciudad(), validos.referencia());
        direccion.setUsuarioCreacion(actor.getEmail());
        direccion.setFechaCreacion(LocalDateTime.now(reloj));
        try {
            bloquearUsuario(idActor);
            direccionEnvioDAO.insertar(direccion);
            if (validos.predeterminada()) {
                if (!direccionEnvioDAO.establecerPredeterminado(idActor, direccion.getId(), actor.getEmail())) {
                    throw new ConflictoException("La direccion no puede elegirse como predeterminada.");
                }
                direccion.setPredeterminada(true);
            }
            DireccionEnvioDTO respuesta = aDTO(direccion);
            TransactionContext.commit();
            return respuesta;
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo registrar la direccion.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public DireccionEnvioDTO modificar(int idActor, int idDireccion, DatosDireccionEnvioDTO datos)
            throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        DatosDireccionEnvioDTO validos = validarFormulario(datos);
        try {
            bloquearUsuario(idActor);
            DireccionEnvio direccion = direccionEnvioDAO.leerParaActualizar(idDireccion);
            comprobarPropiedad(direccion, actor);
            direccion.setAlias(validos.alias());
            direccion.setCalle(validos.calle());
            direccion.setDistrito(validos.distrito());
            direccion.setCiudad(validos.ciudad());
            direccion.setReferencia(validos.referencia());
            direccion.setUsuarioActualizacion(actor.getEmail());
            direccionEnvioDAO.modificar(direccion);
            if (validos.predeterminada() && !direccion.isPredeterminada()) {
                if (!direccionEnvioDAO.establecerPredeterminado(idActor, idDireccion, actor.getEmail())) {
                    throw new ConflictoException("La direccion no puede elegirse como predeterminada.");
                }
                direccion.setPredeterminada(true);
            } else if (!validos.predeterminada() && direccion.isPredeterminada()) {
                if (!direccionEnvioDAO.quitarPredeterminado(idDireccion, actor.getEmail())) {
                    throw new ConflictoException("La direccion predeterminada cambio mientras se editaba.");
                }
                direccion.setPredeterminada(false);
            }
            DireccionEnvioDTO respuesta = aDTO(direccion);
            TransactionContext.commit();
            return respuesta;
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo guardar la direccion.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public DireccionEnvioDTO consultar(int idActor, int idDireccion) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            DireccionEnvio direccion = direccionEnvioDAO.leer(idDireccion);
            comprobarPropiedad(direccion, actor);
            return aDTO(direccion);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la direccion.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<DireccionEnvioDTO> listar(int idActor, int pagina, int tamanioPagina)
            throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        if (pagina < 1 || tamanioPagina < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        try {
            return direccionEnvioDAO.listarPorUsuario(idActor, pagina, tamanioPagina);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las direcciones.", e);
        }
    }

    @Override
    public void eliminar(int idActor, int idDireccion) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            comprobarPropiedad(direccionEnvioDAO.leer(idDireccion), actor);
            if (!direccionEnvioDAO.eliminar(idDireccion, actor.getEmail())) {
                throw new ConflictoException("La direccion ya se habia eliminado.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar la direccion.", e);
        }
    }

    @Override
    public void establecerPredeterminado(int idActor, int idDireccion) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            bloquearUsuario(idActor);
            comprobarPropiedad(direccionEnvioDAO.leerParaActualizar(idDireccion), actor);
            if (!direccionEnvioDAO.establecerPredeterminado(idActor, idDireccion, actor.getEmail())) {
                throw new ConflictoException("La direccion no puede elegirse como predeterminada.");
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo elegir la direccion predeterminada.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    private DatosDireccionEnvioDTO validarFormulario(DatosDireccionEnvioDTO datos) throws ValidacionException {
        if (datos == null) throw new ValidacionException("direccion", "Los datos de la direccion son obligatorios.");
        String alias = datos.alias() == null ? "" : datos.alias().strip();
        if (alias.isEmpty()) throw new ValidacionException("alias", "Este campo es obligatorio.");
        if (alias.length() > 100)
            throw new ValidacionException("alias", "El máximo es 100 caracteres.");
        String calle = datos.calle() == null ? "" : datos.calle().strip();
        if (calle.isEmpty()) throw new ValidacionException("calle", "Este campo es obligatorio.");
        if (calle.length() > 255)
            throw new ValidacionException("calle", "El máximo es 255 caracteres.");
        String distrito = datos.distrito() == null ? "" : datos.distrito().strip();
        if (distrito.isEmpty()) throw new ValidacionException("distrito", "Este campo es obligatorio.");
        if (distrito.length() > 100)
            throw new ValidacionException("distrito", "El máximo es 100 caracteres.");
        String ciudad = datos.ciudad() == null ? "" : datos.ciudad().strip();
        if (ciudad.isEmpty()) throw new ValidacionException("ciudad", "Este campo es obligatorio.");
        if (ciudad.length() > 100)
            throw new ValidacionException("ciudad", "El máximo es 100 caracteres.");
        String referencia = datos.referencia() == null ? null : datos.referencia().strip();
        if (referencia != null && referencia.isEmpty()) referencia = null;
        if (referencia != null && referencia.length() > 255)
            throw new ValidacionException("referencia", "El máximo es 255 caracteres.");
        return new DatosDireccionEnvioDTO(alias,
                calle, distrito,
                ciudad,
                referencia, datos.predeterminada());
    }

    private void bloquearUsuario(int idActor) throws SQLException, NegocioException {
        Usuario usuario = usuarioDAO.leerParaActualizar(idActor);
        if (usuario == null || !usuario.estaDisponible()) {
            throw new CuentaInactivaException("La cuenta ya no esta activa.");
        }
    }

    private void comprobarPropiedad(DireccionEnvio direccion, Usuario actor) throws NegocioException {
        if (direccion == null || direccion.isEliminado()) {
            throw new RecursoNoEncontradoException("La direccion no existe.");
        }
        if (direccion.getUsuario().getId() != actor.getId()) {
            throw new AccesoDenegadoException("La direccion pertenece a otro usuario.");
        }
        if (!direccion.isActivo()) throw new ConflictoException("La direccion no esta disponible.");
    }

    private DireccionEnvioDTO aDTO(DireccionEnvio direccion) {
        return new DireccionEnvioDTO(direccion.getId(), direccion.getAlias(), direccion.getCalle(),
                direccion.getDistrito(), direccion.getCiudad(), direccion.getReferencia(), direccion.isPredeterminada());
    }
}
