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
import java.util.ArrayList;
import java.util.List;

public class SeguimientoDAOImpl implements SeguimientoDAO {

    // Solo las entidades activas (RF04.2), con la misma forma que la búsqueda de Explorar
    @Override
    public int contar(int idUsuario, TipoEntidad tipo, String texto) throws SQLException {
        String sql = """
            select count(*)
            from seguimiento_entidad s
            join entidad e on e.id_entidad = s.fid_entidad
            where s.fid_usuario = ? and e.tipo = ? and e.activo = 1
        """;
        if (texto != null) {
            sql += " and e.nombre like concat('%', ?, '%')";
        }
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            int i = 0;
            pstm.setInt(++i, idUsuario);
            pstm.setString(++i, tipo.name());
            if (texto != null) {
                pstm.setString(++i, texto);
            }
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // orden: "fecha" (seguido recientemente), "nombre-desc" o, por defecto, por nombre ascendente
    @Override
    public List<Entidad> listarPagina(int idUsuario, TipoEntidad tipo, String texto, String orden, int pagina, int tamano) throws SQLException {
        String sql = """
            select e.id_entidad, e.tipo, e.nombre, e.descripcion, e.imagen, e.activo
            from seguimiento_entidad s
            join entidad e on e.id_entidad = s.fid_entidad
            where s.fid_usuario = ? and e.tipo = ? and e.activo = 1
        """;
        if (texto != null) {
            sql += " and e.nombre like concat('%', ?, '%')";
        }
        if ("fecha".equals(orden)) {
            sql += " order by s.fecha desc";
        } else if ("nombre-desc".equals(orden)) {
            sql += " order by e.nombre desc";
        } else {
            sql += " order by e.nombre";
        }
        sql += " limit ?, ?";
        List<Entidad> entidades = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            int i = 0;
            pstm.setInt(++i, idUsuario);
            pstm.setString(++i, tipo.name());
            if (texto != null) {
                pstm.setString(++i, texto);
            }
            pstm.setInt(++i, (pagina - 1) * tamano);
            pstm.setInt(++i, tamano);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Entidad entidad = new Entidad();
                    entidad.setId(rs.getInt("id_entidad"));
                    entidad.setTipo(TipoEntidad.valueOf(rs.getString("tipo")));
                    entidad.setNombre(rs.getString("nombre"));
                    entidad.setDescripcion(rs.getString("descripcion"));
                    entidad.setImagen(rs.getString("imagen"));
                    entidad.setActivo(rs.getBoolean("activo"));
                    entidades.add(entidad);
                }
            }
        }
        return entidades;
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
