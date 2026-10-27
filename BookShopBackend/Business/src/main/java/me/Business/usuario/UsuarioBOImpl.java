package me.Business.usuario;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.CredencialesInvalidasException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.Business.ProteccionDatos;
import me.Business.Validaciones;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.ImagenDTO;
import me.Model.dto.usuario.AccesoUsuarioDTO;
import me.Model.dto.usuario.CambioContraseniaDTO;
import me.Model.dto.usuario.CredencialesDTO;
import me.Model.dto.usuario.DatosPerfilDTO;
import me.Model.dto.usuario.PerfilUsuarioDTO;
import me.Model.dto.usuario.RegistroUsuarioDTO;
import me.Model.entities.usuario.Usuario;

public class UsuarioBOImpl implements UsuarioBO {
    private final UsuarioDAO usuarios;
    private final Clock reloj;
    private final Autorizacion autorizacion;

    public UsuarioBOImpl() {
        this(new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public UsuarioBOImpl(UsuarioDAO usuarios, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.reloj = Objects.requireNonNull(reloj).withZone(ZoneId.of("America/Lima"));
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public AccesoUsuarioDTO registrar(RegistroUsuarioDTO datos) throws NegocioException {
        if (datos == null) throw new ValidacionException("registro", "Los datos del registro son obligatorios.");
        String nombre = Validaciones.texto(datos.nombre(), "nombre", 250);
        String email = correo(datos.email());
        comprobarNacimiento(datos.fechaNacimiento());
        comprobarNuevaContrasenia(datos.contrasenia(), datos.confirmacionContrasenia());
        Validaciones.imagen(datos.imagen());
        try {
            if (usuarios.obtenerPorEmail(email) != null) {
                throw new ConflictoException("El correo ya pertenece a una cuenta.");
            }
            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre(nombre);
            usuario.setFechaNacimiento(datos.fechaNacimiento().atStartOfDay());
            usuario.setContrasenia(ProteccionDatos.hash(datos.contrasenia()));
            usuario.asignarRolPorCorreo();
            usuario.setUsuarioCreacion(email);
            usuario.setFechaCreacion(LocalDateTime.now(reloj));
            if (datos.imagen() != null) {
                usuario.setImagen(datos.imagen().contenido());
                usuario.setTipoImagen(datos.imagen().tipoMime());
            }
            usuarios.insertar(usuario);
            return new AccesoUsuarioDTO(usuario.getId(), usuario.getNombre(), usuario.getRol(), false);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || "23505".equals(e.getSQLState())) {
                throw new ConflictoException("El correo ya pertenece a una cuenta.");
            }
            throw new PersistenciaException("No se pudo registrar la cuenta.", e);
        }
    }

    @Override
    public AccesoUsuarioDTO iniciarSesion(CredencialesDTO credenciales) throws NegocioException {
        Usuario usuario = comprobarCredenciales(credenciales);
        return new AccesoUsuarioDTO(usuario.getId(), usuario.getNombre(), usuario.getRol(), !usuario.isActivo());
    }

    @Override
    public AccesoUsuarioDTO reactivar(CredencialesDTO credenciales) throws NegocioException {
        Usuario usuario = comprobarCredenciales(credenciales);
        try {
            if (!usuario.isActivo() && !usuarios.cambiarActivo(usuario.getId(), true, usuario.getEmail())) {
                throw new ConflictoException("La cuenta cambio mientras se intentaba reactivarla.");
            }
            return new AccesoUsuarioDTO(usuario.getId(), usuario.getNombre(), usuario.getRol(), false);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo reactivar la cuenta.", e);
        }
    }

    private Usuario comprobarCredenciales(CredencialesDTO credenciales) throws NegocioException {
        if (credenciales == null || credenciales.contrasenia() == null) {
            throw new CredencialesInvalidasException("El correo o la contrasenia no son correctos.");
        }
        String email = correo(credenciales.email());
        try {
            Usuario usuario = usuarios.obtenerPorEmail(email);
            if (usuario == null || usuario.isEliminado()
                    || !ProteccionDatos.verificar(credenciales.contrasenia(), usuario.getContrasenia())) {
                throw new CredencialesInvalidasException("El correo o la contrasenia no son correctos.");
            }
            return usuario;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron comprobar las credenciales.", e);
        }
    }

    @Override
    public PerfilUsuarioDTO obtenerPerfil(int idActor) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        try {
            PerfilUsuarioDTO perfil = usuarios.obtenerPerfil(idActor);
            if (perfil == null) throw new RecursoNoEncontradoException("La cuenta no existe.");
            return perfil;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el perfil.", e);
        }
    }

    @Override
    public PerfilUsuarioDTO modificarPerfil(int idActor, DatosPerfilDTO datos) throws NegocioException {
        Usuario usuario = autorizacion.usuarioActivo(idActor);
        if (datos == null) throw new ValidacionException("perfil", "Los datos del perfil son obligatorios.");
        String nombre = Validaciones.texto(datos.nombre(), "nombre", 250);
        comprobarNacimiento(datos.fechaNacimiento());
        usuario.setNombre(nombre);
        usuario.setFechaNacimiento(datos.fechaNacimiento().atStartOfDay());
        usuario.setUsuarioActualizacion(usuario.getEmail());
        try {
            usuarios.modificar(usuario);
            return usuarios.obtenerPerfil(idActor);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el perfil.", e);
        }
    }

    @Override
    public PerfilUsuarioDTO cambiarImagen(int idActor, ImagenDTO imagen) throws NegocioException {
        Usuario usuario = autorizacion.usuarioActivo(idActor);
        Validaciones.imagen(imagen);
        try {
            if (!usuarios.cambiarImagen(idActor, imagen, usuario.getEmail())) {
                throw new ConflictoException("La cuenta ya no permite cambiar la imagen.");
            }
            return usuarios.obtenerPerfil(idActor);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la imagen del perfil.", e);
        }
    }

    @Override
    public void cambiarContrasenia(int idActor, CambioContraseniaDTO datos) throws NegocioException {
        Usuario usuario = autorizacion.usuarioActivo(idActor);
        if (datos == null) throw new ValidacionException("contrasenia", "Los datos de contrasenia son obligatorios.");
        if (datos.contraseniaActual() == null
                || !ProteccionDatos.verificar(datos.contraseniaActual(), usuario.getContrasenia())) {
            throw new CredencialesInvalidasException("La contrasenia actual no es correcta.");
        }
        comprobarNuevaContrasenia(datos.contraseniaNueva(), datos.confirmacionContrasenia());
        String hash = ProteccionDatos.hash(datos.contraseniaNueva());
        try {
            if (!usuarios.cambiarContrasenia(idActor, hash, usuario.getEmail())) {
                throw new ConflictoException("La cuenta ya no permite cambiar la contrasenia.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la contrasenia.", e);
        }
    }

    @Override
    public void desactivar(int idActor, String contraseniaActual) throws NegocioException {
        Usuario usuario = autorizacion.usuarioActivo(idActor);
        if (contraseniaActual == null || !ProteccionDatos.verificar(contraseniaActual, usuario.getContrasenia())) {
            throw new CredencialesInvalidasException("La contrasenia actual no es correcta.");
        }
        try {
            if (!usuarios.cambiarActivo(idActor, false, usuario.getEmail())) {
                throw new ConflictoException("La cuenta ya estaba desactivada.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo desactivar la cuenta.", e);
        }
    }

    @Override
    public void cerrarSesion(int idActor, Duration tiempoSesion) throws NegocioException {
        Usuario usuario = autorizacion.usuarioActivo(idActor);
        if (tiempoSesion == null || tiempoSesion.isNegative()) {
            throw new ValidacionException("tiempoSesion", "El tiempo de la sesion no puede ser negativo.");
        }
        try {
            usuarios.actualizarTiempoTotal(idActor, tiempoSesion, usuario.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el tiempo de la sesion.", e);
        }
    }

    private String correo(String valor) throws ValidacionException {
        String email = Validaciones.texto(valor, "email", 254).toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+")) {
            throw new ValidacionException("email", "El correo no tiene un formato valido.");
        }
        return email;
    }

    private void comprobarNacimiento(LocalDate fecha) throws ValidacionException {
        if (fecha == null || fecha.isAfter(LocalDate.now(reloj))) {
            throw new ValidacionException("fechaNacimiento", "La fecha de nacimiento debe existir y no estar en el futuro.");
        }
    }

    private void comprobarNuevaContrasenia(String nueva, String confirmacion) throws ValidacionException {
        if (nueva == null || nueva.length() < 8) {
            throw new ValidacionException("contrasenia", "La contrasenia debe tener al menos ocho caracteres.");
        }
        if (!nueva.equals(confirmacion)) {
            throw new ValidacionException("confirmacionContrasenia", "La confirmacion debe coincidir con la contrasenia.");
        }
    }
}
