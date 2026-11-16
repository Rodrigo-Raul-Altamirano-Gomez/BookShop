package me.DAO.catalogo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Libro;

public class DescuentoDAOImpl implements DescuentoDAO {

    @Override
    public void insertar(Descuento descuento) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "INSERT INTO DESCUENTO (id_libro,porcentaje,fecha_inicio,fecha_fin,activo," +
            " usuario_creacion,fecha_creacion) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, descuento.getLibro().getId());
            sentencia.setBigDecimal(2, descuento.getPorcentaje());
            sentencia.setTimestamp(3, Timestamp.valueOf(descuento.getFechaInicio()));
            sentencia.setTimestamp(4, Timestamp.valueOf(descuento.getFechaFin()));
            sentencia.setBoolean(5, descuento.isActivo());
            sentencia.setString(6, descuento.getUsuarioCreacion());
            sentencia.setTimestamp(7, Timestamp.valueOf(descuento.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next())
                    throw new SQLException("No se obtuvo el id del descuento.");
                descuento.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public void modificar(Descuento descuento) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE DESCUENTO SET porcentaje=?,fecha_inicio=?,fecha_fin=?,activo=?," +
            " usuario_actualizacion=?,fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBigDecimal(1, descuento.getPorcentaje());
            sentencia.setTimestamp(2, Timestamp.valueOf(descuento.getFechaInicio()));
            sentencia.setTimestamp(3, Timestamp.valueOf(descuento.getFechaFin()));
            sentencia.setBoolean(4, descuento.isActivo());
            sentencia.setString(5, descuento.getUsuarioActualizacion());
            sentencia.setInt(6, descuento.getId());
            sentencia.executeUpdate();
        }
    }

    @Override
    public Descuento leer(int id) throws SQLException {
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM DESCUENTO WHERE id=?")) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapearDescuento(resultado) : null;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE DESCUENTO SET eliminado=TRUE,activo=FALSE,usuario_actualizacion=?," +
            " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<Descuento> listarPorLibro(int idLibro, int pagina, int
        tamanioPagina) throws SQLException {
        List<Descuento> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement contar =
                conexion.prepareStatement("SELECT COUNT(*) FROM DESCUENTO WHERE id_libro=? AND eliminado=FALSE")) {
                contar.setInt(1, idLibro);
                try (ResultSet resultado = contar.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            String sql = "SELECT * FROM DESCUENTO" +
                " WHERE id_libro=? AND eliminado=FALSE ORDER BY fecha_inicio DESC,id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idLibro);
                sentencia.setInt(2, tamanioPagina);
                sentencia.setLong(3, (long) (pagina - 1) * tamanioPagina);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next())
                        items.add(mapearDescuento(resultado));
                }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, pagina, tamanioPagina);
    }

    @Override
    public Descuento obtenerVigente(int idLibro, LocalDateTime fecha) throws SQLException {
        String sql = "SELECT * FROM DESCUENTO WHERE id_libro=? AND eliminado=FALSE" +
            " AND fecha_inicio<=? AND fecha_fin>=? ORDER BY fecha_inicio DESC,id DESC LIMIT 1";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idLibro);
            sentencia.setTimestamp(2, Timestamp.valueOf(fecha));
            sentencia.setTimestamp(3, Timestamp.valueOf(fecha));
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapearDescuento(resultado) : null;
            }
        }
    }

    @Override
    public boolean haySuperposicion(int idLibro, int idExcluir, LocalDateTime inicio, LocalDateTime fin)
        throws SQLException {
        String sql = "SELECT 1 FROM DESCUENTO WHERE id_libro=? AND id<>? AND eliminado=FALSE" +
            " AND fecha_inicio<=? AND fecha_fin>=? LIMIT 1";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idLibro);
            sentencia.setInt(2, idExcluir);
            sentencia.setTimestamp(3, Timestamp.valueOf(fin));
            sentencia.setTimestamp(4, Timestamp.valueOf(inicio));
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private Descuento mapearDescuento(ResultSet resultado) throws SQLException {
        Descuento descuento = new Descuento();
        descuento.setId(resultado.getInt("id"));
        Libro libro = new Libro();
        libro.setId(resultado.getInt("id_libro"));
        descuento.setLibro(libro);
        descuento.setPorcentaje(resultado.getBigDecimal("porcentaje"));
        descuento.setFechaInicio(resultado.getTimestamp("fecha_inicio").toLocalDateTime());
        descuento.setFechaFin(resultado.getTimestamp("fecha_fin").toLocalDateTime());
        descuento.setActivo(resultado.getBoolean("activo"));
        descuento.setEliminado(resultado.getBoolean("eliminado"));
        descuento.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        descuento.setFechaCreacion(resultado.getTimestamp("fecha_creacion").toLocalDateTime());
        descuento.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        Timestamp actualizacion = resultado.getTimestamp("fecha_actualizacion");
        descuento.setFechaActualizacion(actualizacion == null ? null : actualizacion.toLocalDateTime());
        return descuento;
    }
}
