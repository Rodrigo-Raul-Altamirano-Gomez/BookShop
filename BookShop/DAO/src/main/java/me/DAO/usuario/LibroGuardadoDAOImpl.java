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
        String sql = "INSERT INTO LIBRO_GUARDADO (id_usuario, id_libro, favorito, eliminado, "
                + "usuario_creacion, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?)";
        if (guardado.getFechaCreacion() == null) guardado.setFechaCreacion(LocalDateTime.now());
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, guardado.getUsuario().getId());
            statement.setInt(2, guardado.getLibro().getId());
            statement.setBoolean(3, guardado.isFavorito());
            statement.setBoolean(4, guardado.isEliminado());
            statement.setString(5, guardado.getUsuarioCreacion());
            statement.setTimestamp(6, Timestamp.valueOf(guardado.getFechaCreacion()));
            statement.executeUpdate();
            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se genero el identificador del libro guardado.");
                guardado.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public LibroGuardado leer(int id) throws SQLException {
        String sql = "SELECT * FROM LIBRO_GUARDADO WHERE id = ?";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) return null;
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
        String sql = "UPDATE LIBRO_GUARDADO SET eliminado = TRUE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public void guardar(int idUsuario, int idLibro, boolean favorito, String usuario) throws SQLException {
        // La clave unica permite recuperar una relacion retirada sin duplicarla.
        String sql = "INSERT INTO LIBRO_GUARDADO (id_usuario, id_libro, favorito, usuario_creacion) "
                + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "usuario_actualizacion = CASE WHEN eliminado THEN ? ELSE usuario_actualizacion END, "
                + "fecha_actualizacion = CASE WHEN eliminado THEN CURRENT_TIMESTAMP ELSE fecha_actualizacion END, "
                + "eliminado = FALSE";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idLibro);
            statement.setBoolean(3, favorito);
            statement.setString(4, usuario);
            statement.setString(5, usuario);
            statement.executeUpdate();
        }
    }

    @Override
    public boolean retirar(int idUsuario, int idLibro, boolean favorito, String usuario) throws SQLException {
        String sql = "UPDATE LIBRO_GUARDADO SET eliminado = TRUE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_usuario = ? AND id_libro = ? "
                + "AND favorito = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, idUsuario);
            statement.setInt(3, idLibro);
            statement.setBoolean(4, favorito);
            return statement.executeUpdate() == 1;
        }
    }
}
