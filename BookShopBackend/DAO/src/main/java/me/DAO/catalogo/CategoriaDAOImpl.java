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
import me.Model.entities.catalogo.Categoria;

public class CategoriaDAOImpl implements CategoriaDAO {

    @Override
    public void insertar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO CATEGORIA" +
            " (nombre,descripcion,imagen,activo,usuario_creacion,fecha_creacion)" + " VALUES (?,?,?,?,?,?)";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, categoria.getNombre());
            sentencia.setString(2, categoria.getDescripcion());
            sentencia.setString(3, categoria.getImagen());
            sentencia.setBoolean(4, categoria.isActivo());
            sentencia.setString(5, categoria.getUsuarioCreacion());
            sentencia.setTimestamp(6, Timestamp.valueOf(categoria.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next())
                    throw new SQLException("No se obtuvo el id del categoria.");
                categoria.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public void modificar(Categoria categoria) throws SQLException {
        String sql = "UPDATE CATEGORIA SET nombre=?,descripcion=?,usuario_actualizacion=?," +
            " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, categoria.getNombre());
            sentencia.setString(2, categoria.getDescripcion());
            sentencia.setString(3, categoria.getUsuarioActualizacion());
            sentencia.setInt(4, categoria.getId());
            sentencia.executeUpdate();
        }
    }

    @Override
    public Categoria leer(int id) throws SQLException {
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM CATEGORIA WHERE id=?")) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next())
                    return null;
                Categoria categoria = new Categoria();
                categoria.setId(resultado.getInt("id"));
                categoria.setNombre(resultado.getString("nombre"));
                categoria.setDescripcion(resultado.getString("descripcion"));
                categoria.setImagen(resultado.getString("imagen"));
                categoria.setActivo(resultado.getBoolean("activo"));
                categoria.setEliminado(resultado.getBoolean("eliminado"));
                categoria.setUsuarioCreacion(resultado.getString("usuario_creacion"));
                categoria.setFechaCreacion(resultado.getTimestamp("fecha_creacion").toLocalDateTime());
                categoria.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
                Timestamp actualizacion = resultado.getTimestamp("fecha_actualizacion");
                categoria.setFechaActualizacion(actualizacion == null ? null : actualizacion.toLocalDateTime());
                return categoria;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE CATEGORIA SET eliminado=TRUE,activo=FALSE,usuario_actualizacion=?," +
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
        String sql = "UPDATE CATEGORIA SET" +
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
        String sql = "UPDATE CATEGORIA SET imagen=?,usuario_actualizacion=?," +
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
        // En LIKE, estos caracteres deben buscarse como texto literal.
        String textoBuscado = nombre.replace("!", "!!");
        textoBuscado = textoBuscado.replace("%", "!%");
        textoBuscado = textoBuscado.replace("_", "!_");
        textoBuscado = "%" + textoBuscado + "%";
        String condiciones = " WHERE eliminado=FALSE" + (filtro.soloActivos() ? " AND activo=TRUE" : "") +
            (nombre.isEmpty() ? "" : " AND LOWER(nombre) LIKE LOWER(?) ESCAPE '!'");
        String orden = filtro.orden() == OrdenReferencia.MAS_RECIENTES ? "fecha_creacion DESC,id DESC" :
            "nombre ASC,id ASC";
        String columnas = "id,nombre,imagen";
        String sql = "SELECT " + columnas + " FROM CATEGORIA" + condiciones + " ORDER BY " + orden +
            " LIMIT ? OFFSET ?";
        List<TarjetaReferenciaDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement contar = conexion.prepareStatement("SELECT COUNT(*) FROM CATEGORIA" +
                condiciones)) {
                if (!nombre.isEmpty())
                    contar.setString(1, textoBuscado);
                try (ResultSet resultado = contar.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                int parametro = 1;
                if (!nombre.isEmpty())
                    sentencia.setString(parametro++, textoBuscado);
                sentencia.setInt(parametro++, filtro.tamanioPagina());
                sentencia.setLong(parametro, (long) (filtro.pagina() - 1) * filtro.tamanioPagina());
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
            " s.id_categoria=a.id" + " AND s.tipo_seguimiento='CATEGORIA' AND s.eliminado=FALSE) AS seguido" +
            " FROM CATEGORIA a WHERE a.id=? AND a.eliminado=FALSE";
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
            " JOIN CATEGORIA a ON a.id=l.id_categoria WHERE p.eliminado=FALSE AND i.eliminado=FALSE" +
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
        String sql = "SELECT 1 FROM CATEGORIA WHERE nombre=? AND id<>? LIMIT 1";
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
