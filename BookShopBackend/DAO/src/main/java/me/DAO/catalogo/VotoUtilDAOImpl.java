package me.DAO.catalogo;

import java.sql.*;
import me.DBManager.TransactionContext;

public class VotoUtilDAOImpl implements VotoUtilDAO {
    @Override
    public boolean cambiarVoto(int idUsuario, int idResena, boolean marcar, String actor) throws SQLException {
        // La reseña serializa cambios del contador y recuperaciones de la relación.
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT id FROM RESENA WHERE id = ? AND activo AND NOT eliminado FOR UPDATE")) {
            sentencia.setInt(1, idResena);
            try (ResultSet resultado = sentencia.executeQuery()) { if (!resultado.next()) return false; }
        }
        int idVoto = 0;
        boolean marcado = false;
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT id, eliminado FROM VOTO_UTIL WHERE id_usuario = ? AND id_resena = ? FOR UPDATE")) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, idResena);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    idVoto = resultado.getInt("id");
                    marcado = !resultado.getBoolean("eliminado");
                }
            }
        }
        if (marcado == marcar) return false;
        if (idVoto == 0) {
            try (PreparedStatement sentencia = conexion.prepareStatement("INSERT INTO VOTO_UTIL (id_usuario, id_resena, usuario_creacion) VALUES (?, ?, ?)")) {
                sentencia.setInt(1, idUsuario);
                sentencia.setInt(2, idResena);
                sentencia.setString(3, actor);
                if (sentencia.executeUpdate() != 1) throw new SQLException("No se pudo registrar el voto.");
            }
        } else {
            try (PreparedStatement sentencia = conexion.prepareStatement("UPDATE VOTO_UTIL SET eliminado = ?, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?")) {
                sentencia.setBoolean(1, !marcar);
                sentencia.setString(2, actor);
                sentencia.setInt(3, idVoto);
                if (sentencia.executeUpdate() != 1) throw new SQLException("No se pudo actualizar el voto.");
            }
        }
        try (PreparedStatement sentencia = conexion.prepareStatement("UPDATE RESENA SET cantidad_votos_utiles = cantidad_votos_utiles + ?, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?")) {
            sentencia.setInt(1, marcar ? 1 : -1);
            sentencia.setString(2, actor);
            sentencia.setInt(3, idResena);
            if (sentencia.executeUpdate() != 1) throw new SQLException("No se pudo actualizar el contador de votos.");
        }
        return true;
    }
}
