package me.Business.usuario;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.CredencialesInvalidasException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.ProteccionDatos;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.usuario.AccesoUsuarioDTO;
import me.Model.dto.usuario.CambioContraseniaDTO;
import me.Model.dto.usuario.CredencialesDTO;
import me.Model.dto.usuario.DatosPerfilDTO;
import me.Model.dto.usuario.PerfilUsuarioDTO;
import me.Model.dto.usuario.RegistroUsuarioDTO;
import me.Model.entities.usuario.Usuario;

public class UsuarioBOImpl implements UsuarioBO {

    private final UsuarioDAO usuarioDAO;
    private final Clock reloj;

    public UsuarioBOImpl() {
        this(new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public UsuarioBOImpl(UsuarioDAO usuarioDAO, Clock reloj) {
        this.usuarioDAO = usuarioDAO;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public AccesoUsuarioDTO registrar(RegistroUsuarioDTO datos) throws NegocioException {
        if (datos == null)
            throw new ValidacionException("registro", "Los datos del registro son obligatorios.");
        String nombre = datos.nombre() == null ? "" : datos.nombre().strip();
        if (nombre.isEmpty())
            throw new ValidacionException("nombre", "Este campo es obligatorio.");
        if (nombre.length() > 250)
            throw new ValidacionException("nombre", "El máximo es 250 caracteres.");
        String email = correo(datos.email());
        if (datos.fechaNacimiento() == null || datos.fechaNacimiento().isAfter(LocalDate.now(reloj))) {
            throw new ValidacionException("fechaNacimiento",
                "La datos.fechaNacimiento() de nacimiento debe existir y no estar en el futuro.");
        }
        if (datos.contrasenia() == null || datos.contrasenia().length() < 8) {
            throw new ValidacionException("contrasenia", "La contrasenia debe tener al menos ocho caracteres.");
        }
        if (!datos.contrasenia().equals(datos.confirmacionContrasenia())) {
            throw new ValidacionException("confirmacionContrasenia",
                "La datos.confirmacionContrasenia() debe coincidir con la contrasenia.");
        }
        if (datos.imagen() != null && datos.imagen().length() > 2048)
            throw new ValidacionException("imagen", "La URL admite hasta 2048 caracteres.");
        try {
            if (usuarioDAO.obtenerPorEmail(email) != null) {
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
            usuario.setImagen(datos.imagen());
            usuarioDAO.insertar(usuario);
            return new AccesoUsuarioDTO(usuario.getId(), usuario.getNombre(), usuario.getRol(), false);
        } catch (SQLException e) {
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
            if (!usuario.isActivo() && !usuarioDAO.cambiarActivo(usuario.getId(), true, usuario.getEmail())) {
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
            Usuario usuario = usuarioDAO.obtenerPorEmail(email);
            if (usuario == null || usuario.isEliminado() ||
                !ProteccionDatos.verificar(credenciales.contrasenia(), usuario.getContrasenia())) {
                throw new CredencialesInvalidasException("El correo o la contrasenia no son correctos.");
            }
            return usuario;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron comprobar las credenciales.", e);
        }
    }

    @Override
    public void comprobarActivo(int idActor) throws NegocioException {
        try {
            Usuario usuario = usuarioDAO.leer(idActor);
            if (usuario == null || !usuario.estaDisponible())
                throw new CuentaInactivaException("La cuenta está desactivada.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar la cuenta.", e);
        }
    }

    @Override
    public PerfilUsuarioDTO obtenerPerfil(int idActor) throws NegocioException {
        try {
            PerfilUsuarioDTO perfil = usuarioDAO.obtenerPerfil(idActor);
            if (perfil == null)
                throw new RecursoNoEncontradoException("La cuenta no existe.");
            return perfil;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el perfil.", e);
        }
    }

    @Override
    public PerfilUsuarioDTO modificarPerfil(int idActor, DatosPerfilDTO datos) throws NegocioException {
        try {
            Usuario usuario = usuarioDAO.leer(idActor);
            if (datos == null)
                throw new ValidacionException("perfil", "Los datos del perfil son obligatorios.");
            String nombre = datos.nombre() == null ? "" : datos.nombre().strip();
            if (nombre.isEmpty())
                throw new ValidacionException("nombre", "Este campo es obligatorio.");
            if (nombre.length() > 250)
                throw new ValidacionException("nombre", "El máximo es 250 caracteres.");
            if (datos.fechaNacimiento() == null || datos.fechaNacimiento().isAfter(LocalDate.now(reloj))) {
                throw new ValidacionException("fechaNacimiento",
                    "La datos.fechaNacimiento() de nacimiento debe existir y no estar en el futuro.");
            }
            usuario.setNombre(nombre);
            usuario.setFechaNacimiento(datos.fechaNacimiento().atStartOfDay());
            usuario.setUsuarioActualizacion(usuario.getEmail());
            usuarioDAO.modificar(usuario);
            return usuarioDAO.obtenerPerfil(idActor);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el perfil.", e);
        }
    }

    @Override
    public PerfilUsuarioDTO cambiarImagen(int idActor, String imagen) throws NegocioException {
        try {
            Usuario usuario = usuarioDAO.leer(idActor);
            if (imagen != null && imagen.length() > 2048)
                throw new ValidacionException("imagen", "La URL admite hasta 2048 caracteres.");
            if (!usuarioDAO.cambiarImagen(idActor, imagen, usuario.getEmail())) {
                throw new ConflictoException("La cuenta ya no permite cambiar la imagen.");
            }
            return usuarioDAO.obtenerPerfil(idActor);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la imagen del perfil.", e);
        }
    }

    @Override
    public void cambiarContrasenia(int idActor, CambioContraseniaDTO datos) throws NegocioException {
        try {
            Usuario usuario = usuarioDAO.leer(idActor);
            if (datos == null)
                throw new ValidacionException("contrasenia", "Los datos de contrasenia son obligatorios.");
            if (datos.contraseniaActual() == null || !ProteccionDatos.verificar(datos.contraseniaActual(),
                usuario.getContrasenia())) {
                throw new CredencialesInvalidasException("La contrasenia actual no es correcta.");
            }
            if (datos.contraseniaNueva() == null || datos.contraseniaNueva().length() < 8) {
                throw new ValidacionException("contrasenia",
                    "La contrasenia debe tener al menos ocho caracteres.");
            }
            if (!datos.contraseniaNueva().equals(datos.confirmacionContrasenia())) {
                throw new ValidacionException("confirmacionContrasenia",
                    "La datos.confirmacionContrasenia() debe coincidir con la contrasenia.");
            }
            String hash = ProteccionDatos.hash(datos.contraseniaNueva());
            if (!usuarioDAO.cambiarContrasenia(idActor, hash, usuario.getEmail())) {
                throw new ConflictoException("La cuenta ya no permite cambiar la contrasenia.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la contrasenia.", e);
        }
    }

    @Override
    public void desactivar(int idActor, String contraseniaActual) throws NegocioException {
        try {
            Usuario usuario = usuarioDAO.leer(idActor);
            if (contraseniaActual == null || !ProteccionDatos.verificar(contraseniaActual,
                usuario.getContrasenia())) {
                throw new CredencialesInvalidasException("La contrasenia actual no es correcta.");
            }
            if (!usuarioDAO.cambiarActivo(idActor, false, usuario.getEmail())) {
                throw new ConflictoException("La cuenta ya estaba desactivada.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo desactivar la cuenta.", e);
        }
    }

    @Override
    public void cerrarSesion(int idActor, Duration tiempoSesion) throws NegocioException {
        try {
            Usuario usuario = usuarioDAO.leer(idActor);
            if (tiempoSesion == null || tiempoSesion.isNegative()) {
                throw new ValidacionException("tiempoSesion", "El tiempo de la sesion no puede ser negativo.");
            }
            usuarioDAO.actualizarTiempoTotal(idActor, tiempoSesion, usuario.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el tiempo de la sesion.", e);
        }
    }

    private String correo(String valor) throws ValidacionException {
        String email = valor == null ? "" : valor.strip();
        if (email.isEmpty())
            throw new ValidacionException("email", "Este campo es obligatorio.");
        if (email.length() > 254)
            throw new ValidacionException("email", "El máximo es 254 caracteres.");
        email = email.toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+")) {
            throw new ValidacionException("email", "El correo no tiene un formato valido.");
        }
        return email;
    }
}
