package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.ResenaDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.Resena;
import pe.edu.pucp.bookshop.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResenaDAOImpl implements ResenaDAO {

    @Override
    public List<Resena> listarPorLibro(int idLibro, int idUsuario) throws SQLException {
        String sql = """
            select r.id_resena, r.puntuacion, r.comentario, r.fecha, r.activa,
                   u.id_usuario, u.nombre_completo, u.foto,
                   (select count(*) from voto_util v where v.fid_resena = r.id_resena) as votos,
                   (select count(*) from voto_util v where v.fid_resena = r.id_resena and v.fid_usuario = ?) as votada
            from resena r
            join usuario u on u.id_usuario = r.fid_usuario
            where r.fid_libro = ?
            order by r.fecha desc
        """;
        List<Resena> resenas = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idLibro);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Resena resena = new Resena();
                    resena.setId(rs.getInt("id_resena"));
                    resena.setPuntuacion(rs.getInt("puntuacion"));
                    resena.setComentario(rs.getString("comentario"));
                    resena.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    resena.setActiva(rs.getBoolean("activa"));
                    resena.setVotosUtiles(rs.getInt("votos"));
                    resena.setVotadaPorMi(rs.getInt("votada") > 0);
                    Usuario usuario = new Usuario();
                    usuario.setId(rs.getInt("id_usuario"));
                    usuario.setNombreCompleto(rs.getString("nombre_completo"));
                    usuario.setFoto(rs.getString("foto"));
                    resena.setUsuario(usuario);
                    resenas.add(resena);
                }
            }
        }
        return resenas;
    }

    @Override
    public List<Resena> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = """
            select r.id_resena, r.puntuacion, r.comentario, r.fecha, r.activa,
                   l.id_libro, l.titulo, l.portada, a.nombre as nombre_autor
            from resena r
            join libro l on l.id_libro = r.fid_libro
            join entidad a on a.id_entidad = l.fid_autor
            where r.fid_usuario = ?
            order by r.fecha desc
        """;
        List<Resena> resenas = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Resena resena = new Resena();
                    resena.setId(rs.getInt("id_resena"));
                    resena.setPuntuacion(rs.getInt("puntuacion"));
                    resena.setComentario(rs.getString("comentario"));
                    resena.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    resena.setActiva(rs.getBoolean("activa"));
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setPortada(rs.getString("portada"));
                    Entidad autor = new Entidad();
                    autor.setNombre(rs.getString("nombre_autor"));
                    libro.setAutor(autor);
                    resena.setLibro(libro);
                    resenas.add(resena);
                }
            }
        }
        return resenas;
    }

    @Override
    public List<Resena> listarActivas() throws SQLException {
        String sql = "select id_resena, fid_libro, puntuacion, fecha from resena where activa = 1";
        List<Resena> resenas = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {
            while (rs.next()) {
                Resena resena = new Resena();
                resena.setId(rs.getInt("id_resena"));
                Libro libro = new Libro();
                libro.setId(rs.getInt("fid_libro"));
                resena.setLibro(libro);
                resena.setPuntuacion(rs.getInt("puntuacion"));
                resena.setFecha(rs.getObject("fecha", LocalDateTime.class));
                resenas.add(resena);
            }
        }
        return resenas;
    }

    @Override
    public Resena obtener(int id) throws SQLException {
        String sql = "select id_resena, fid_usuario, fid_libro, puntuacion, comentario, fecha, activa from resena where id_resena = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    Resena resena = new Resena();
                    resena.setId(rs.getInt("id_resena"));
                    Usuario usuario = new Usuario();
                    usuario.setId(rs.getInt("fid_usuario"));
                    resena.setUsuario(usuario);
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("fid_libro"));
                    resena.setLibro(libro);
                    resena.setPuntuacion(rs.getInt("puntuacion"));
                    resena.setComentario(rs.getString("comentario"));
                    resena.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    resena.setActiva(rs.getBoolean("activa"));
                    return resena;
                }
            }
        }
        return null;
    }

    @Override
    public int insertar(Resena resena) throws SQLException {
        String sql = "insert into resena (fid_usuario, fid_libro, puntuacion, comentario, fecha, activa) values (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setInt(1, resena.getUsuario().getId());
            pstm.setInt(2, resena.getLibro().getId());
            pstm.setInt(3, resena.getPuntuacion());
            pstm.setString(4, resena.getComentario());
            pstm.setObject(5, resena.getFecha());
            pstm.setBoolean(6, resena.getActiva());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void modificar(Resena resena) throws SQLException {
        String sql = "update resena set puntuacion = ?, comentario = ? where id_resena = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, resena.getPuntuacion());
            pstm.setString(2, resena.getComentario());
            pstm.setInt(3, resena.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public boolean existeVoto(int idResena, int idUsuario) throws SQLException {
        String sql = "select count(*) from voto_util where fid_resena = ? and fid_usuario = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idResena);
            pstm.setInt(2, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public void insertarVoto(int idResena, int idUsuario) throws SQLException {
        String sql = "insert into voto_util (fid_usuario, fid_resena) values (?, ?)";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idResena);
            pstm.executeUpdate();
        }
    }

    @Override
    public void eliminarVoto(int idResena, int idUsuario) throws SQLException {
        String sql = "delete from voto_util where fid_resena = ? and fid_usuario = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idResena);
            pstm.setInt(2, idUsuario);
            pstm.executeUpdate();
        }
    }

    @Override
    public void cambiarActiva(int id, boolean activa) throws SQLException {
        String sql = "update resena set activa = ? where id_resena = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setBoolean(1, activa);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public void eliminarVotos(int idResena) throws SQLException {
        String sql = "delete from voto_util where fid_resena = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idResena);
            pstm.executeUpdate();
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        String sql = "delete from resena where id_resena = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        }
    }
}
