package me.DAO.catalogo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.catalogo.DetalleReferenciaDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.OrdenReferencia;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.entities.catalogo.Editorial;

public class EditorialDAOImpl implements EditorialDAO {

    @Override
    public void insertar(Editorial editorial) throws SQLException {
        String sql = "INSERT INTO EDITORIAL" +
            " (nombre,descripcion,imagen,activo,usuario_creacion,fecha_creacion)" + " VALUES (?,?,?,?,?,?)";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, editorial.getNombre());
            sentencia.setString(2, editorial.getDescripcion());
            sentencia.setString(3, editorial.getImagen());
            sentencia.setBoolean(4, editorial.isActivo());
            sentencia.setString(5, editorial.getUsuarioCreacion());
            sentencia.setTimestamp(6, Timestamp.valueOf(editorial.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next())
                    throw new SQLException("No se obtuvo el id del editorial.");
                editorial.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public void modificar(Editorial editorial) throws SQLException {
        String sql = "UPDATE EDITORIAL SET nombre=?,descripcion=?,usuario_actualizacion=?," +
            " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, editorial.getNombre());
            sentencia.setString(2, editorial.getDescripcion());
            sentencia.setString(3, editorial.getUsuarioActualizacion());
            sentencia.setInt(4, editorial.getId());
            sentencia.executeUpdate();
        }
    }

    @Override
    public Editorial leer(int id) throws SQLException {
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM EDITORIAL WHERE id=?")) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next())
                    return null;
                Editorial editorial = new Editorial();
                editorial.setId(resultado.getInt("id"));
                editorial.setNombre(resultado.getString("nombre"));
                editorial.setDescripcion(resultado.getString("descripcion"));
                editorial.setImagen(resultado.getString("imagen"));
                editorial.setActivo(resultado.getBoolean("activo"));
                editorial.setEliminado(resultado.getBoolean("eliminado"));
                editorial.setUsuarioCreacion(resultado.getString("usuario_creacion"));
                editorial.setFechaCreacion(resultado.getTimestamp("fecha_creacion").toLocalDateTime());
                editorial.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
                Timestamp actualizacion = resultado.getTimestamp("fecha_actualizacion");
                editorial.setFechaActualizacion(actualizacion == null ? null : actualizacion.toLocalDateTime());
                return editorial;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE EDITORIAL SET eliminado=TRUE,activo=FALSE,usuario_actualizacion=?," +
            " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cambiarActivo(int id, boolean activo, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE EDITORIAL SET" +
            " activo=?,usuario_actualizacion=?,fecha_actualizacion=CURRENT_TIMESTAMP" +
            " WHERE id=? AND eliminado=FALSE AND activo<>?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBoolean(1, activo);
            sentencia.setString(2, usuario);
            sentencia.setInt(3, id);
            sentencia.setBoolean(4, activo);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cambiarImagen(int id, String imagen, String usuario) throws SQLException {
        String sql = "UPDATE EDITORIAL SET imagen=?,usuario_actualizacion=?," +
            " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, imagen);
            sentencia.setString(2, usuario);
            sentencia.setInt(3, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(FiltroReferenciaDTO filtro) throws SQLException {
        String nombre = filtro.nombre() == null ? "" : filtro.nombre().strip();
        String condiciones = " WHERE eliminado=FALSE" + (filtro.soloActivos() ? " AND activo=TRUE" : "") +
            " AND LOCATE(LOWER(?),LOWER(nombre))>0";
        String orden = filtro.orden() == OrdenReferencia.MAS_RECIENTES ? "fecha_creacion DESC,id DESC" :
            "nombre ASC,id ASC";
        String sql = "SELECT id,nombre,imagen FROM EDITORIAL" + condiciones + " ORDER BY " + orden +
            " LIMIT ? OFFSET ?";
        List<TarjetaReferenciaDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement contar = conexion.prepareStatement("SELECT COUNT(*) FROM EDITORIAL" +
                condiciones)) {
                contar.setString(1, nombre);
                try (ResultSet resultado = contar.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, nombre);
                sentencia.setInt(2, filtro.tamanioPagina());
                sentencia.setLong(3, (long) (filtro.pagina() - 1) * filtro.tamanioPagina());
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        String imagen = resultado.getString("imagen");
                        items.add(new TarjetaReferenciaDTO(resultado.getInt("id"),
                            resultado.getString("nombre"), imagen));
                    }
                }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, filtro.pagina(), filtro.tamanioPagina());
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int id, int idUsuario) throws SQLException {
        String sql = "SELECT a.*,EXISTS(SELECT 1 FROM SEGUIMIENTO s WHERE s.id_usuario=? AND" +
            " s.id_editorial=a.id" + " AND s.tipo_seguimiento='EDITORIAL' AND s.eliminado=FALSE) AS seguido" +
            " FROM EDITORIAL a WHERE a.id=? AND a.eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next())
                    return null;
                TarjetaReferenciaDTO tarjeta = new TarjetaReferenciaDTO(resultado.getInt("id"),
                    resultado.getString("nombre"), resultado.getString("imagen"));
                return new DetalleReferenciaDTO(tarjeta, resultado.getString("descripcion"),
                    resultado.getBoolean("activo"), resultado.getBoolean("seguido"));
            }
        }
    }

    @Override
    public TarjetaReferenciaDTO obtenerMasComprada(int idUsuario, LocalDate inicio, LocalDate fin) throws SQLException {
        String sql = "SELECT a.id,a.nombre,SUM(i.cantidad) AS ejemplares FROM PEDIDO p" +
            " JOIN ITEM_PEDIDO i ON i.id_pedido=p.id JOIN LIBRO l ON l.id=i.id_libro" +
            " JOIN EDITORIAL a ON a.id=l.id_editorial WHERE p.eliminado=FALSE AND i.eliminado=FALSE" +
            " AND p.estado NOT IN ('CANCELADO','DEVUELTO') AND p.fecha_pago>=? AND p.fecha_pago<?" +
            " AND p.id_usuario=? GROUP BY a.id,a.nombre" +
            " ORDER BY ejemplares DESC,a.nombre ASC,a.id ASC LIMIT 1";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            sentencia.setTimestamp(2, Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));
            sentencia.setInt(3, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? new TarjetaReferenciaDTO(resultado.getInt("id"),
                    resultado.getString("nombre"), null) : null;
            }
        }
    }

    @Override
    public boolean existeNombre(String nombre, int idExcluir) throws SQLException {
        String sql = "SELECT 1 FROM EDITORIAL WHERE nombre=? AND id<>? LIMIT 1";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombre);
            sentencia.setInt(2, idExcluir);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next();
            }
        }
    }
}
