package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.EntidadDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.TipoEntidad;
import pe.edu.pucp.bookshop.model.dto.EntidadAdmin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EntidadDAOImpl implements EntidadDAO {

    @Override
    public int contar(TipoEntidad tipo, String texto, Boolean activo) throws SQLException {
        String sql = "select count(*) from entidad where tipo = ?";
        if (texto != null) {
            sql += " and nombre like concat('%', ?, '%')";
        }
        if (activo != null) {
            sql += " and activo = ?";
        }
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            int i = 0;
            pstm.setString(++i, tipo.name());
            if (texto != null) {
                pstm.setString(++i, texto);
            }
            if (activo != null) {
                pstm.setBoolean(++i, activo);
            }
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public List<Entidad> listarPagina(TipoEntidad tipo, String texto, Boolean activo, boolean descendente, int pagina, int tamano) throws SQLException {
        String sql = "select id_entidad, tipo, nombre, descripcion, imagen, activo from entidad where tipo = ?";
        if (texto != null) {
            sql += " and nombre like concat('%', ?, '%')";
        }
        if (activo != null) {
            sql += " and activo = ?";
        }
        if (descendente) {
            sql += " order by nombre desc";
        } else {
            sql += " order by nombre";
        }
        sql += " limit ?, ?";
        List<Entidad> entidades = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            int i = 0;
            pstm.setString(++i, tipo.name());
            if (texto != null) {
                pstm.setString(++i, texto);
            }
            if (activo != null) {
                pstm.setBoolean(++i, activo);
            }
            pstm.setInt(++i, (pagina - 1) * tamano);
            pstm.setInt(++i, tamano);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    entidades.add(mapear(rs));
                }
            }
        }
        return entidades;
    }

    @Override
    public List<EntidadAdmin> listarPaginaAdmin(TipoEntidad tipo, String texto, Boolean activo, int pagina, int tamano) throws SQLException {
        String sql = """
            select e.id_entidad, e.tipo, e.nombre, e.descripcion, e.imagen, e.activo,
                   (select count(*) from libro l
                    where l.fid_autor = e.id_entidad or l.fid_categoria = e.id_entidad or l.fid_editorial = e.id_entidad) as cantidad_libros
            from entidad e
            where e.tipo = ?
        """;
        if (texto != null) {
            sql += " and e.nombre like concat('%', ?, '%')";
        }
        if (activo != null) {
            sql += " and e.activo = ?";
        }
        sql += " order by e.nombre limit ?, ?";
        List<EntidadAdmin> filas = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            int i = 0;
            pstm.setString(++i, tipo.name());
            if (texto != null) {
                pstm.setString(++i, texto);
            }
            if (activo != null) {
                pstm.setBoolean(++i, activo);
            }
            pstm.setInt(++i, (pagina - 1) * tamano);
            pstm.setInt(++i, tamano);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    filas.add(new EntidadAdmin(mapear(rs), rs.getInt("cantidad_libros")));
                }
            }
        }
        return filas;
    }

    @Override
    public List<Entidad> listarActivas(TipoEntidad tipo) throws SQLException {
        String sql = """
            select id_entidad, tipo, nombre, descripcion, imagen, activo
            from entidad
            where tipo = ? and activo = 1
            order by nombre
        """;
        List<Entidad> entidades = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, tipo.name());
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    entidades.add(mapear(rs));
                }
            }
        }
        return entidades;
    }

    @Override
    public Entidad obtener(int id) throws SQLException {
        String sql = "select id_entidad, tipo, nombre, descripcion, imagen, activo from entidad where id_entidad = ?";
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
    public int insertar(Entidad entidad) throws SQLException {
        String sql = "insert into entidad (tipo, nombre, descripcion, imagen, activo) values (?, ?, ?, ?, ?)";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, entidad.getTipo().name());
            pstm.setString(2, entidad.getNombre());
            pstm.setString(3, entidad.getDescripcion());
            pstm.setString(4, entidad.getImagen());
            pstm.setBoolean(5, entidad.getActivo());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void modificar(Entidad entidad) throws SQLException {
        String sql = "update entidad set nombre = ?, descripcion = ?, imagen = ? where id_entidad = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, entidad.getNombre());
            pstm.setString(2, entidad.getDescripcion());
            pstm.setString(3, entidad.getImagen());
            pstm.setInt(4, entidad.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void cambiarActivo(int id, boolean activo) throws SQLException {
        String sql = "update entidad set activo = ? where id_entidad = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setBoolean(1, activo);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    private Entidad mapear(ResultSet rs) throws SQLException {
        Entidad entidad = new Entidad();
        entidad.setId(rs.getInt("id_entidad"));
        entidad.setTipo(TipoEntidad.valueOf(rs.getString("tipo")));
        entidad.setNombre(rs.getString("nombre"));
        entidad.setDescripcion(rs.getString("descripcion"));
        entidad.setImagen(rs.getString("imagen"));
        entidad.setActivo(rs.getBoolean("activo"));
        return entidad;
    }
}
