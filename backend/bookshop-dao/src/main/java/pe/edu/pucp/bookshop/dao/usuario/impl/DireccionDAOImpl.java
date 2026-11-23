package pe.edu.pucp.bookshop.dao.usuario.impl;

import pe.edu.pucp.bookshop.dao.usuario.DireccionDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.usuario.Direccion;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DireccionDAOImpl implements DireccionDAO {

    @Override
    public List<Direccion> listarActivasPorUsuario(int idUsuario) throws SQLException {
        String sql = """
            select id_direccion, fid_usuario, alias, direccion, distrito, ciudad, predeterminada, activa
            from direccion
            where fid_usuario = ? and activa = 1
            order by predeterminada desc, id_direccion
        """;
        List<Direccion> direcciones = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Direccion direccion = new Direccion();
                    direccion.setId(rs.getInt("id_direccion"));
                    Usuario usuario = new Usuario();
                    usuario.setId(rs.getInt("fid_usuario"));
                    direccion.setUsuario(usuario);
                    direccion.setAlias(rs.getString("alias"));
                    direccion.setDireccion(rs.getString("direccion"));
                    direccion.setDistrito(rs.getString("distrito"));
                    direccion.setCiudad(rs.getString("ciudad"));
                    direccion.setPredeterminada(rs.getBoolean("predeterminada"));
                    direccion.setActiva(rs.getBoolean("activa"));
                    direcciones.add(direccion);
                }
            }
        }
        return direcciones;
    }

    @Override
    public int insertar(Direccion direccion) throws SQLException {
        String sql = """
            insert into direccion (fid_usuario, alias, direccion, distrito, ciudad, predeterminada, activa)
            values (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setInt(1, direccion.getUsuario().getId());
            pstm.setString(2, direccion.getAlias());
            pstm.setString(3, direccion.getDireccion());
            pstm.setString(4, direccion.getDistrito());
            pstm.setString(5, direccion.getCiudad());
            pstm.setBoolean(6, direccion.getPredeterminada());
            pstm.setBoolean(7, direccion.getActiva());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void modificar(Direccion direccion) throws SQLException {
        String sql = "update direccion set alias = ?, direccion = ?, distrito = ?, ciudad = ? where id_direccion = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, direccion.getAlias());
            pstm.setString(2, direccion.getDireccion());
            pstm.setString(3, direccion.getDistrito());
            pstm.setString(4, direccion.getCiudad());
            pstm.setInt(5, direccion.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void desactivar(int id) throws SQLException {
        String sql = "update direccion set activa = 0, predeterminada = 0 where id_direccion = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public void quitarPredeterminada(int idUsuario) throws SQLException {
        String sql = "update direccion set predeterminada = 0 where fid_usuario = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.executeUpdate();
        }
    }

    @Override
    public void marcarPredeterminada(int id) throws SQLException {
        String sql = "update direccion set predeterminada = 1 where id_direccion = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        }
    }
}
