package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.MetodoPagoDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.MetodoPago;
import pe.edu.pucp.bookshop.model.TipoMetodoPago;
import pe.edu.pucp.bookshop.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MetodoPagoDAOImpl implements MetodoPagoDAO {

    @Override
    public List<MetodoPago> listarActivosPorUsuario(int idUsuario) throws SQLException {
        String sql = """
            select id_metodo_pago, fid_usuario, tipo, titular, numero, fecha_vencimiento, proveedor, predeterminado, activo
            from metodo_pago
            where fid_usuario = ? and activo = 1
            order by predeterminado desc, id_metodo_pago
        """;
        List<MetodoPago> metodos = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    MetodoPago metodo = new MetodoPago();
                    metodo.setId(rs.getInt("id_metodo_pago"));
                    Usuario usuario = new Usuario();
                    usuario.setId(rs.getInt("fid_usuario"));
                    metodo.setUsuario(usuario);
                    metodo.setTipo(TipoMetodoPago.valueOf(rs.getString("tipo")));
                    metodo.setTitular(rs.getString("titular"));
                    metodo.setNumero(rs.getString("numero"));
                    metodo.setFechaVencimiento(rs.getString("fecha_vencimiento"));
                    metodo.setProveedor(rs.getString("proveedor"));
                    metodo.setPredeterminado(rs.getBoolean("predeterminado"));
                    metodo.setActivo(rs.getBoolean("activo"));
                    metodos.add(metodo);
                }
            }
        }
        return metodos;
    }

    @Override
    public int contarActivosConNumero(int idUsuario, String numero, int idExcluido) throws SQLException {
        String sql = """
            select count(*) from metodo_pago
            where fid_usuario = ? and numero = ? and activo = 1 and id_metodo_pago <> ?
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setString(2, numero);
            pstm.setInt(3, idExcluido);
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public int insertar(MetodoPago metodo) throws SQLException {
        String sql = """
            insert into metodo_pago (fid_usuario, tipo, titular, numero, fecha_vencimiento, proveedor, predeterminado, activo)
            values (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setInt(1, metodo.getUsuario().getId());
            pstm.setString(2, metodo.getTipo().name());
            pstm.setString(3, metodo.getTitular());
            pstm.setString(4, metodo.getNumero());
            pstm.setString(5, metodo.getFechaVencimiento());
            pstm.setString(6, metodo.getProveedor());
            pstm.setBoolean(7, metodo.getPredeterminado());
            pstm.setBoolean(8, metodo.getActivo());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void modificar(MetodoPago metodo) throws SQLException {
        String sql = """
            update metodo_pago set tipo = ?, titular = ?, numero = ?, fecha_vencimiento = ?, proveedor = ?
            where id_metodo_pago = ?
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, metodo.getTipo().name());
            pstm.setString(2, metodo.getTitular());
            pstm.setString(3, metodo.getNumero());
            pstm.setString(4, metodo.getFechaVencimiento());
            pstm.setString(5, metodo.getProveedor());
            pstm.setInt(6, metodo.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void desactivar(int id) throws SQLException {
        String sql = "update metodo_pago set activo = 0, predeterminado = 0 where id_metodo_pago = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public void quitarPredeterminado(int idUsuario) throws SQLException {
        String sql = "update metodo_pago set predeterminado = 0 where fid_usuario = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.executeUpdate();
        }
    }

    @Override
    public void marcarPredeterminado(int id) throws SQLException {
        String sql = "update metodo_pago set predeterminado = 1 where id_metodo_pago = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        }
    }
}
