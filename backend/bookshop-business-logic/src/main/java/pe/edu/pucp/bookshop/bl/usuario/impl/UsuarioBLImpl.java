package pe.edu.pucp.bookshop.bl.usuario.impl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.CredencialesInvalidasException;
import pe.edu.pucp.bookshop.bl.exception.CuentaDesactivadaException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.exception.RegistroDuplicadoException;
import pe.edu.pucp.bookshop.bl.usuario.UsuarioBL;
import pe.edu.pucp.bookshop.dao.usuario.UsuarioDAO;
import pe.edu.pucp.bookshop.dao.usuario.impl.UsuarioDAOImpl;
import pe.edu.pucp.bookshop.model.usuario.CambioContrasena;
import pe.edu.pucp.bookshop.model.usuario.Rol;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HexFormat;

public class UsuarioBLImpl implements UsuarioBL {

    private UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    @Override
    public Usuario registrar(Usuario usuario) throws BusinessLogicException {
        try {
            // RN01: correo único y rol según el dominio
            if (usuarioDAO.obtenerPorCorreo(usuario.getCorreo()) != null) {
                throw new RegistroDuplicadoException("Ya existe una cuenta con este correo.");
            }
            if (usuario.getCorreo().endsWith("@admin.bookshop")) {
                usuario.setRol(Rol.ADMINISTRADOR);
            } else {
                usuario.setRol(Rol.CLIENTE);
            }
            usuario.setContrasena(hash(usuario.getContrasena()));
            usuario.setActivo(true);
            usuario.setFechaRegistro(LocalDateTime.now().withNano(0));
            usuario.setId(usuarioDAO.insertar(usuario));
            usuario.setContrasena(null);
            return usuario;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Usuario login(String correo, String contrasena) throws BusinessLogicException {
        try {
            Usuario usuario = usuarioDAO.obtenerPorCorreo(correo);
            if (usuario == null || !usuario.getContrasena().equals(hash(contrasena))) {
                throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
            }
            // RN02: la cuenta desactivada no inicia sesión hasta reactivarla
            if (!usuario.getActivo()) {
                throw new CuentaDesactivadaException("Tu cuenta está desactivada.");
            }
            usuario.setContrasena(null);
            return usuario;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Usuario reactivar(String correo, String contrasena) throws BusinessLogicException {
        try {
            // RN02: se reactiva con su contraseña
            Usuario usuario = usuarioDAO.obtenerPorCorreo(correo);
            if (usuario == null || !usuario.getContrasena().equals(hash(contrasena))) {
                throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
            }
            usuarioDAO.cambiarActivo(usuario.getId(), true);
            usuario.setActivo(true);
            usuario.setContrasena(null);
            return usuario;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Usuario obtener(int id) throws BusinessLogicException {
        try {
            Usuario usuario = usuarioDAO.obtener(id);
            usuario.setContrasena(null);
            return usuario;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Usuario modificar(Usuario usuario) throws BusinessLogicException {
        try {
            usuarioDAO.modificar(usuario);
            Usuario actualizado = usuarioDAO.obtener(usuario.getId());
            actualizado.setContrasena(null);
            return actualizado;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void cambiarContrasena(int id, CambioContrasena cambio) throws BusinessLogicException {
        try {
            Usuario usuario = usuarioDAO.obtener(id);
            if (!usuario.getContrasena().equals(hash(cambio.actual()))) {
                throw new CredencialesInvalidasException("La contraseña actual no es correcta.");
            }
            usuarioDAO.cambiarContrasena(id, hash(cambio.nueva()));
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void desactivar(int id) throws BusinessLogicException {
        try {
            usuarioDAO.cambiarActivo(id, false);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // SHA-256 en hexadecimal; lo usan el registro, el inicio de sesión, la reactivación y el cambio de contraseña
    private String hash(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}
