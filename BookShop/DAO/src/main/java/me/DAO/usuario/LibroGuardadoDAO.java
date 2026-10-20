package me.DAO.usuario;

import java.sql.SQLException;
import me.Model.entities.usuario.LibroGuardado;

public interface LibroGuardadoDAO {
    void insertar(LibroGuardado guardado) throws SQLException;
    LibroGuardado leer(int id) throws SQLException;
    boolean eliminar(int id, String usuario) throws SQLException;
    void guardar(int idUsuario, int idLibro, boolean favorito, String usuario) throws SQLException;
    boolean retirar(int idUsuario, int idLibro, boolean favorito, String usuario) throws SQLException;
}
