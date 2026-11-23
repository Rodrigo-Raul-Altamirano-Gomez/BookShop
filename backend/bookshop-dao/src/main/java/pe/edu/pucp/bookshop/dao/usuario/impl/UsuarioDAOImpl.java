package pe.edu.pucp.bookshop.dao.usuario.impl;

import pe.edu.pucp.bookshop.dao.usuario.UsuarioDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.model.usuario.Rol;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public Usuario obtener(int id) throws SQLException {
        String sql = """
            select id_usuario, correo, nombre_completo, fecha_nacimiento, contrasena, foto, rol, activo, fecha_registro
            from usuario
            where id_usuario = ?
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Usuario obtenerPorCorreo(String correo) throws SQLException {
        String sql = """
            select id_usuario, correo, nombre_completo, fecha_nacimiento, contrasena, foto, rol, activo, fecha_registro
            from usuario
            where correo = ?
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, correo);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    @Override
    public int insertar(Usuario usuario) throws SQLException {
        String sql = """
            insert into usuario (correo, nombre_completo, fecha_nacimiento, contrasena, foto, rol, activo, fecha_registro)
            values (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, usuario.getCorreo());
            pstm.setString(2, usuario.getNombreCompleto());
            pstm.setObject(3, usuario.getFechaNacimiento());
            pstm.setString(4, usuario.getContrasena());
            pstm.setString(5, usuario.getFoto());
            pstm.setString(6, usuario.getRol().name());
            pstm.setBoolean(7, usuario.getActivo());
            pstm.setObject(8, usuario.getFechaRegistro());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void modificar(Usuario usuario) throws SQLException {
        String sql = "update usuario set nombre_completo = ?, fecha_nacimiento = ?, foto = ? where id_usuario = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, usuario.getNombreCompleto());
            pstm.setObject(2, usuario.getFechaNacimiento());
            pstm.setString(3, usuario.getFoto());
            pstm.setInt(4, usuario.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void cambiarContrasena(int id, String contrasena) throws SQLException {
        String sql = "update usuario set contrasena = ? where id_usuario = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, contrasena);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public void cambiarActivo(int id, boolean activo) throws SQLException {
        String sql = "update usuario set activo = ? where id_usuario = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setBoolean(1, activo);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public List<Usuario> listarPorRol(Rol rol) throws SQLException {
        String sql = """
            select id_usuario, correo, nombre_completo, fecha_nacimiento, contrasena, foto, rol, activo, fecha_registro
            from usuario
            where rol = ?
        """;
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, rol.name());
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(mapear(rs));
                }
            }
        }
        return usuarios;
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario"));
        usuario.setCorreo(rs.getString("correo"));
        usuario.setNombreCompleto(rs.getString("nombre_completo"));
        usuario.setFechaNacimiento(rs.getObject("fecha_nacimiento", LocalDate.class));
        usuario.setContrasena(rs.getString("contrasena"));
        usuario.setFoto(rs.getString("foto"));
        usuario.setRol(Rol.valueOf(rs.getString("rol")));
        usuario.setActivo(rs.getBoolean("activo"));
        usuario.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        return usuario;
    }
}
