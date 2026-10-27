package me.DAO.catalogo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DetalleReferenciaDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.OrdenReferencia;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.entities.catalogo.Categoria;

public class CategoriaDAOImpl implements CategoriaDAO {
    @Override
    public void insertar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO CATEGORIA (nombre,descripcion,imagen,tipo_imagen,activo,usuario_creacion,fecha_creacion)"
                + " VALUES (?,?,?,?,?,?,?)";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, categoria.getNombre());
            sentencia.setString(2, categoria.getDescripcion());
            sentencia.setBytes(3, categoria.getImagen());
            sentencia.setString(4, categoria.getTipoImagen());
            sentencia.setBoolean(5, categoria.isActivo());
            sentencia.setString(6, categoria.getUsuarioCreacion());
            sentencia.setTimestamp(7, Timestamp.valueOf(categoria.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se obtuvo el id del categoria.");
                categoria.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public void modificar(Categoria categoria) throws SQLException {
        String sql = "UPDATE CATEGORIA SET nombre=?,descripcion=?,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
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
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    @Override
    public Categoria leerParaActualizar(int id) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM CATEGORIA WHERE id=? FOR UPDATE")) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE CATEGORIA SET eliminado=TRUE,activo=FALSE,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, id);
            if (sentencia.executeUpdate() != 1) return false;
        }
        new LibroDAOImpl().recalcularVetosPorCategoria(id, usuario);
        return true;
    }

    @Override
    public boolean cambiarActivo(int id, boolean activo, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE CATEGORIA SET activo=?,usuario_actualizacion=?,fecha_actualizacion=CURRENT_TIMESTAMP"
                + " WHERE id=? AND eliminado=FALSE AND activo<>?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBoolean(1, activo);
            sentencia.setString(2, usuario);
            sentencia.setInt(3, id);
            sentencia.setBoolean(4, activo);
            if (sentencia.executeUpdate() != 1) return false;
        }
        new LibroDAOImpl().recalcularVetosPorCategoria(id, usuario);
        return true;
    }

    @Override
    public boolean cambiarImagen(int id, ImagenDTO imagen, String usuario) throws SQLException {
        String sql = "UPDATE CATEGORIA SET imagen=?,tipo_imagen=?,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBytes(1, imagen == null ? null : imagen.contenido());
            sentencia.setString(2, imagen == null ? null : imagen.tipoMime());
            sentencia.setString(3, usuario);
            sentencia.setInt(4, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(FiltroReferenciaDTO filtro) throws SQLException {
        return buscar(filtro, true);
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscarOpciones(FiltroReferenciaDTO filtro) throws SQLException {
        return buscar(filtro, false);
    }

    private ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(FiltroReferenciaDTO filtro, boolean conImagen) throws SQLException {
        if (filtro == null || filtro.pagina() < 1 || filtro.tamanioPagina() < 1 || filtro.orden() == null) {
            throw new IllegalArgumentException("Filtro, orden y paginacion validos son obligatorios.");
        }
        String nombre = filtro.nombre() == null ? "" : filtro.nombre().trim();
        String condiciones = " WHERE eliminado=FALSE" + (filtro.soloActivos() ? " AND activo=TRUE" : "")
                + (nombre.isEmpty() ? "" : " AND LOWER(nombre) LIKE LOWER(?) ESCAPE '!'");
        String orden = filtro.orden() == OrdenReferencia.MAS_RECIENTES
                ? "fecha_creacion DESC,id DESC" : "nombre ASC,id ASC";
        String columnas = conImagen ? "id,nombre,imagen,tipo_imagen" : "id,nombre";
        String sql = "SELECT " + columnas + " FROM CATEGORIA" + condiciones + " ORDER BY " + orden + " LIMIT ? OFFSET ?";
        List<TarjetaReferenciaDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement contar = conexion.prepareStatement("SELECT COUNT(*) FROM CATEGORIA" + condiciones)) {
                if (!nombre.isEmpty()) contar.setString(1, patron(nombre));
                try (ResultSet resultado = contar.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                int parametro = 1;
                if (!nombre.isEmpty()) sentencia.setString(parametro++, patron(nombre));
                sentencia.setInt(parametro++, filtro.tamanioPagina());
                sentencia.setLong(parametro, (long) (filtro.pagina() - 1) * filtro.tamanioPagina());
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        ImagenDTO imagen = conImagen ? imagen(resultado) : null;
                        items.add(new TarjetaReferenciaDTO(resultado.getInt("id"), resultado.getString("nombre"), imagen));
                    }
                }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, filtro.pagina(), filtro.tamanioPagina());
    }

    @Override
    public DetalleReferenciaDTO obtenerDetalle(int id, int idUsuario) throws SQLException {
        String sql = "SELECT a.*,EXISTS(SELECT 1 FROM SEGUIMIENTO s WHERE s.id_usuario=? AND s.id_categoria=a.id"
                + " AND s.tipo_seguimiento='CATEGORIA' AND s.eliminado=FALSE) AS seguido"
                + " FROM CATEGORIA a WHERE a.id=? AND a.eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return null;
                TarjetaReferenciaDTO tarjeta = new TarjetaReferenciaDTO(resultado.getInt("id"),
                        resultado.getString("nombre"), imagen(resultado));
                return new DetalleReferenciaDTO(tarjeta, resultado.getString("descripcion"),
                        resultado.getBoolean("activo"), resultado.getBoolean("seguido"));
            }
        }
    }

    @Override
    public Map<Integer, TarjetaReferenciaDTO> obtenerMasCompradas(List<Integer> idsUsuarios, LocalDate inicio, LocalDate fin)
            throws SQLException {
        if (idsUsuarios == null || inicio == null || fin == null || fin.isBefore(inicio)
                || idsUsuarios.stream().anyMatch(id -> id == null || id < 1)) {
            throw new IllegalArgumentException("Usuarios y periodo validos son obligatorios.");
        }
        Map<Integer, TarjetaReferenciaDTO> referencias = new LinkedHashMap<>();
        if (idsUsuarios.isEmpty()) return referencias;
        String marcadores = String.join(",", java.util.Collections.nCopies(idsUsuarios.size(), "?"));
        String sql = "SELECT p.id_usuario,a.id,a.nombre,SUM(i.cantidad) AS ejemplares FROM PEDIDO p"
                + " JOIN ITEM_PEDIDO i ON i.id_pedido=p.id JOIN LIBRO l ON l.id=i.id_libro"
                + " JOIN CATEGORIA a ON a.id=l.id_categoria WHERE p.eliminado=FALSE AND i.eliminado=FALSE"
                + " AND p.estado NOT IN ('CANCELADO','DEVUELTO') AND p.fecha_pago>=? AND p.fecha_pago<?"
                + " AND p.id_usuario IN (" + marcadores + ") GROUP BY p.id_usuario,a.id,a.nombre"
                + " ORDER BY p.id_usuario,ejemplares DESC,a.nombre ASC,a.id ASC";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            sentencia.setTimestamp(2, Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));
            int parametro = 3;
            for (Integer id : idsUsuarios) sentencia.setInt(parametro++, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    referencias.putIfAbsent(resultado.getInt("id_usuario"), new TarjetaReferenciaDTO(
                            resultado.getInt("id"), resultado.getString("nombre"), null));
                }
            }
        }
        return referencias;
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

    private Categoria mapear(ResultSet resultado) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(resultado.getInt("id"));
        categoria.setNombre(resultado.getString("nombre"));
        categoria.setDescripcion(resultado.getString("descripcion"));
        categoria.setImagen(resultado.getBytes("imagen"));
        categoria.setTipoImagen(resultado.getString("tipo_imagen"));
        categoria.setActivo(resultado.getBoolean("activo"));
        categoria.setEliminado(resultado.getBoolean("eliminado"));
        categoria.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        categoria.setFechaCreacion(resultado.getTimestamp("fecha_creacion").toLocalDateTime());
        categoria.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        Timestamp actualizacion = resultado.getTimestamp("fecha_actualizacion");
        categoria.setFechaActualizacion(actualizacion == null ? null : actualizacion.toLocalDateTime());
        return categoria;
    }

    private ImagenDTO imagen(ResultSet resultado) throws SQLException {
        byte[] contenido = resultado.getBytes("imagen");
        return contenido == null ? null : new ImagenDTO(contenido, resultado.getString("tipo_imagen"));
    }

    private String patron(String nombre) {
        return "%" + nombre.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}

