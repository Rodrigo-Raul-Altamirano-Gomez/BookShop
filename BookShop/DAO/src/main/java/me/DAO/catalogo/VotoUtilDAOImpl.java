package me.DAO.catalogo;

import java.sql.*;
import me.DBManager.TransactionContext;

public class VotoUtilDAOImpl implements VotoUtilDAO {
    @Override
    public boolean cambiarVoto(int idUsuario, int idResena, boolean marcar, String actor) throws SQLException {
        // La reseña serializa cambios del contador y recuperaciones de la relación.
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement("SELECT id FROM RESENA WHERE id = ? AND activo AND NOT eliminado FOR UPDATE")) {
            statement.setInt(1, idResena);
            try (ResultSet rs = statement.executeQuery()) { if (!rs.next()) return false; }
        }
        int idVoto = 0;
        boolean marcado = false;
        try (PreparedStatement statement = conexion.prepareStatement("SELECT id, eliminado FROM VOTO_UTIL WHERE id_usuario = ? AND id_resena = ? FOR UPDATE")) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idResena);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    idVoto = rs.getInt("id");
                    marcado = !rs.getBoolean("eliminado");
                }
            }
        }
        if (marcado == marcar) return false;
        if (idVoto == 0) {
            try (PreparedStatement statement = conexion.prepareStatement("INSERT INTO VOTO_UTIL (id_usuario, id_resena, usuario_creacion) VALUES (?, ?, ?)")) {
                statement.setInt(1, idUsuario);
                statement.setInt(2, idResena);
                statement.setString(3, actor);
                if (statement.executeUpdate() != 1) throw new SQLException("No se pudo registrar el voto.");
            }
        } else {
            try (PreparedStatement statement = conexion.prepareStatement("UPDATE VOTO_UTIL SET eliminado = ?, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?")) {
                statement.setBoolean(1, !marcar);
                statement.setString(2, actor);
                statement.setInt(3, idVoto);
                if (statement.executeUpdate() != 1) throw new SQLException("No se pudo actualizar el voto.");
            }
        }
        try (PreparedStatement statement = conexion.prepareStatement("UPDATE RESENA SET cantidad_votos_utiles = cantidad_votos_utiles + ?, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setInt(1, marcar ? 1 : -1);
            statement.setString(2, actor);
            statement.setInt(3, idResena);
            if (statement.executeUpdate() != 1) throw new SQLException("No se pudo actualizar el contador de votos.");
        }
        return true;
    }
}
