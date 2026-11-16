package me.DAO.usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import me.DBManager.DBManager;

public class LibroGuardadoDAOImpl implements LibroGuardadoDAO {

    @Override
    public void guardar(int idUsuario, int idLibro, boolean favorito, String usuario) throws SQLException {
        // La clave unica permite recuperar una relacion retirada sin duplicarla.
        String sql = "INSERT INTO LIBRO_GUARDADO (id_usuario, id_libro, favorito, usuario_creacion) " +
            "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
            "usuario_actualizacion = CASE WHEN eliminado THEN ? ELSE usuario_actualizacion END, " +
            "fecha_actualizacion = CASE WHEN eliminado THEN CURRENT_TIMESTAMP ELSE fecha_actualizacion END, "
            + "eliminado = FALSE";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, idLibro);
            sentencia.setBoolean(3, favorito);
            sentencia.setString(4, usuario);
            sentencia.setString(5, usuario);
            sentencia.executeUpdate();
        }
    }

    @Override
    public boolean retirar(int idUsuario, int idLibro, boolean favorito, String usuario) throws SQLException {
        String sql = "UPDATE LIBRO_GUARDADO SET eliminado = TRUE, usuario_actualizacion = ?, " +
            "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_usuario = ? AND id_libro = ? " +
            "AND favorito = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, idUsuario);
            sentencia.setInt(3, idLibro);
            sentencia.setBoolean(4, favorito);
            return sentencia.executeUpdate() == 1;
        }
    }
}
