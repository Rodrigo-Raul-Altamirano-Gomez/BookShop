package me.DAO.usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import me.DBManager.DBManager;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.LibroGuardado;
import me.Model.entities.usuario.Usuario;

public class LibroGuardadoDAOImpl implements LibroGuardadoDAO {

    @Override
    public void insertar(LibroGuardado guardado) throws SQLException {
        String sql = "INSERT INTO LIBRO_GUARDADO (id_usuario, id_libro, favorito, eliminado, " +
            "usuario_creacion, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?)";
        if (guardado.getFechaCreacion() == null)
            guardado.setFechaCreacion(LocalDateTime.now());
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, guardado.getUsuario().getId());
            sentencia.setInt(2, guardado.getLibro().getId());
            sentencia.setBoolean(3, guardado.isFavorito());
            sentencia.setBoolean(4, guardado.isEliminado());
            sentencia.setString(5, guardado.getUsuarioCreacion());
            sentencia.setTimestamp(6, Timestamp.valueOf(guardado.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next())
                    throw new SQLException("No se genero el identificador del libro guardado.");
                guardado.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public LibroGuardado leer(int id) throws SQLException {
        String sql = "SELECT * FROM LIBRO_GUARDADO WHERE id = ?";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next())
                    return null;
                LibroGuardado guardado = new LibroGuardado();
                guardado.setId(resultado.getInt("id"));
                Usuario propietario = new Usuario();
                propietario.setId(resultado.getInt("id_usuario"));
                guardado.setUsuario(propietario);
                Libro libro = new Libro();
                libro.setId(resultado.getInt("id_libro"));
                guardado.setLibro(libro);
                guardado.setFavorito(resultado.getBoolean("favorito"));
                guardado.setEliminado(resultado.getBoolean("eliminado"));
                guardado.setUsuarioCreacion(resultado.getString("usuario_creacion"));
                guardado.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
                guardado.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
                guardado.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
                return guardado;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        String sql = "UPDATE LIBRO_GUARDADO SET eliminado = TRUE, usuario_actualizacion = ?, " +
            "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate() == 1;
        }
    }

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
