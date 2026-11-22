package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.SeguimientoDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.SeguimientoEntidad;
import pe.edu.pucp.bookshop.model.TipoEntidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SeguimientoDAOImpl implements SeguimientoDAO {

    @Override
    public List<SeguimientoEntidad> listarPorUsuario(int idUsuario, TipoEntidad tipo) throws SQLException {
        String sql = """
            select s.fecha, e.id_entidad, e.tipo, e.nombre, e.descripcion, e.imagen, e.activo
            from seguimiento_entidad s
            join entidad e on e.id_entidad = s.fid_entidad
            where s.fid_usuario = ? and e.tipo = ?
        """;
        List<SeguimientoEntidad> seguimientos = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setString(2, tipo.name());
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    SeguimientoEntidad seguimiento = new SeguimientoEntidad();
                    seguimiento.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    Entidad entidad = new Entidad();
                    entidad.setId(rs.getInt("id_entidad"));
                    entidad.setTipo(TipoEntidad.valueOf(rs.getString("tipo")));
                    entidad.setNombre(rs.getString("nombre"));
                    entidad.setDescripcion(rs.getString("descripcion"));
                    entidad.setImagen(rs.getString("imagen"));
                    entidad.setActivo(rs.getBoolean("activo"));
                    seguimiento.setEntidad(entidad);
                    seguimientos.add(seguimiento);
                }
            }
        }
        return seguimientos;
    }

    @Override
    public boolean existe(int idUsuario, int idEntidad) throws SQLException {
        String sql = "select count(*) from seguimiento_entidad where fid_usuario = ? and fid_entidad = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idEntidad);
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public void insertar(SeguimientoEntidad seguimiento) throws SQLException {
        String sql = "insert into seguimiento_entidad (fid_usuario, fid_entidad, fecha) values (?, ?, ?)";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, seguimiento.getUsuario().getId());
            pstm.setInt(2, seguimiento.getEntidad().getId());
            pstm.setObject(3, seguimiento.getFecha());
            pstm.executeUpdate();
        }
    }

    @Override
    public void eliminar(int idUsuario, int idEntidad) throws SQLException {
        String sql = "delete from seguimiento_entidad where fid_usuario = ? and fid_entidad = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idEntidad);
            pstm.executeUpdate();
        }
    }
}
