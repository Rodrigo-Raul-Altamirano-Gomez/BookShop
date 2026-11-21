package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.DescuentoDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.model.Descuento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DescuentoDAOImpl implements DescuentoDAO {

    @Override
    public List<Descuento> listarPorLibro(int idLibro) throws SQLException {
        String sql = """
            select id_descuento, porcentaje, fecha_inicio, fecha_fin
            from descuento
            where fid_libro = ?
            order by fecha_inicio
        """;
        List<Descuento> descuentos = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idLibro);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Descuento descuento = new Descuento();
                    descuento.setId(rs.getInt("id_descuento"));
                    descuento.setPorcentaje(rs.getInt("porcentaje"));
                    descuento.setFechaInicio(rs.getObject("fecha_inicio", LocalDate.class));
                    descuento.setFechaFin(rs.getObject("fecha_fin", LocalDate.class));
                    descuentos.add(descuento);
                }
            }
        }
        return descuentos;
    }

    @Override
    public int insertar(Descuento descuento) throws SQLException {
        String sql = "insert into descuento (fid_libro, porcentaje, fecha_inicio, fecha_fin) values (?, ?, ?, ?)";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setInt(1, descuento.getLibro().getId());
            pstm.setInt(2, descuento.getPorcentaje());
            pstm.setObject(3, descuento.getFechaInicio());
            pstm.setObject(4, descuento.getFechaFin());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        String sql = "delete from descuento where id_descuento = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        }
    }
}
