package pe.edu.pucp.bookshop.dao.usuario;

import pe.edu.pucp.bookshop.model.usuario.Rol;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

import java.sql.SQLException;
import java.util.List;

public interface UsuarioDAO {
    Usuario obtener(int id) throws SQLException;
    Usuario obtenerPorCorreo(String correo) throws SQLException;
    List<Usuario> listarPorRol(Rol rol) throws SQLException;
    int insertar(Usuario usuario) throws SQLException;
    void modificar(Usuario usuario) throws SQLException;
    void cambiarContrasena(int id, String contrasena) throws SQLException;
    void cambiarActivo(int id, boolean activo) throws SQLException;
}
