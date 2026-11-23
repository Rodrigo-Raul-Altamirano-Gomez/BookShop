package pe.edu.pucp.bookshop.dao.pedido.impl;

import pe.edu.pucp.bookshop.dao.pedido.CuponDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.pedido.Cupon;
import pe.edu.pucp.bookshop.model.pedido.Pedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CuponDAOImpl implements CuponDAO {

    // Con el pedido en que se usó, si se usó; del que vence antes al que vence después
    @Override
    public List<Cupon> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = """
            select c.id_cupon, c.codigo, c.porcentaje, c.fecha_emision, c.fecha_vencimiento, c.activo, p.id_pedido
            from cupon c
            left join pedido p on p.fid_cupon = c.id_cupon
            where c.fid_usuario = ?
            order by c.fecha_vencimiento, c.id_cupon
        """;
        List<Cupon> cupones = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Cupon cupon = new Cupon();
                    cupon.setId(rs.getInt("id_cupon"));
                    cupon.setCodigo(rs.getString("codigo"));
                    cupon.setPorcentaje(rs.getInt("porcentaje"));
                    cupon.setFechaEmision(rs.getObject("fecha_emision", LocalDate.class));
                    cupon.setFechaVencimiento(rs.getObject("fecha_vencimiento", LocalDate.class));
                    cupon.setActivo(rs.getBoolean("activo"));
                    int idPedido = rs.getInt("id_pedido");
                    if (!rs.wasNull()) {
                        Pedido pedido = new Pedido();
                        pedido.setId(idPedido);
                        cupon.setPedido(pedido);
                    }
                    cupones.add(cupon);
                }
            }
        }
        return cupones;
    }

    @Override
    public Cupon obtener(int id) throws SQLException {
        String sql = "select id_cupon, codigo, porcentaje, fecha_emision, fecha_vencimiento, activo from cupon where id_cupon = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    Cupon cupon = new Cupon();
                    cupon.setId(rs.getInt("id_cupon"));
                    cupon.setCodigo(rs.getString("codigo"));
                    cupon.setPorcentaje(rs.getInt("porcentaje"));
                    cupon.setFechaEmision(rs.getObject("fecha_emision", LocalDate.class));
                    cupon.setFechaVencimiento(rs.getObject("fecha_vencimiento", LocalDate.class));
                    cupon.setActivo(rs.getBoolean("activo"));
                    return cupon;
                }
            }
        }
        return null;
    }

    @Override
    public int insertar(Cupon cupon) throws SQLException {
        String sql = """
            insert into cupon (fid_usuario, codigo, porcentaje, fecha_emision, fecha_vencimiento, activo)
            values (?, ?, ?, ?, ?, ?)
        """;
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setInt(1, cupon.getUsuario().getId());
            pstm.setString(2, cupon.getCodigo());
            pstm.setInt(3, cupon.getPorcentaje());
            pstm.setObject(4, cupon.getFechaEmision());
            pstm.setObject(5, cupon.getFechaVencimiento());
            pstm.setBoolean(6, cupon.getActivo());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void desactivar(int id) throws SQLException {
        String sql = "update cupon set activo = 0 where id_cupon = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        }
    }
}
