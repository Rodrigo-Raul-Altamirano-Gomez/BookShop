package me.DAO.venta;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.CuponDTO;
import me.Model.dto.compra.EstadoCupon;
import me.Model.entities.compra.Cupon;
import me.Model.entities.usuario.Usuario;

public class CuponDAOImpl implements CuponDAO {
    @Override
    public void insertar(Cupon cupon) throws SQLException {
        String sql = """
                INSERT INTO CUPON (id_usuario, porcentaje, monto_minimo, fecha_inicio, fecha_fin,
                    activo, consumido, eliminado, usuario_creacion, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        // La emisión participa en la transacción de la primera entrega.
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, cupon.getUsuario().getId());
            statement.setBigDecimal(2, cupon.getPorcentaje());
            statement.setBigDecimal(3, cupon.getMontoMinimo());
            statement.setTimestamp(4, Timestamp.valueOf(cupon.getFechaInicio()));
            statement.setTimestamp(5, Timestamp.valueOf(cupon.getFechaFin()));
            statement.setBoolean(6, cupon.isActivo());
            statement.setBoolean(7, cupon.isConsumido());
            statement.setBoolean(8, cupon.isEliminado());
            statement.setString(9, cupon.getUsuarioCreacion());
            statement.setTimestamp(10, Timestamp.valueOf(cupon.getFechaCreacion()));
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("No se genero el identificador del cupon.");
                cupon.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public Cupon leer(int id) throws SQLException {
        try (Connection conexion = DBManager.getConnection()) {
            return leerRegistro(conexion, id, false);
        }
    }

    @Override
    public Cupon leerParaActualizar(int id) throws SQLException {
        return leerRegistro(TransactionContext.getConnection(), id, true);
    }

    private Cupon leerRegistro(Connection conexion, int id, boolean bloquear) throws SQLException {
        String sql = "SELECT * FROM CUPON WHERE id = ?" + (bloquear ? " FOR UPDATE" : "");
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapearCupon(rs) : null;
            }
        }
    }

    private Cupon mapearCupon(ResultSet rs) throws SQLException {
        Cupon cupon = new Cupon();
        cupon.setId(rs.getInt("id"));
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario"));
        cupon.setUsuario(usuario);
        cupon.setPorcentaje(rs.getBigDecimal("porcentaje"));
        cupon.setMontoMinimo(rs.getBigDecimal("monto_minimo"));
        cupon.setFechaInicio(rs.getObject("fecha_inicio", LocalDateTime.class));
        cupon.setFechaFin(rs.getObject("fecha_fin", LocalDateTime.class));
        cupon.setActivo(rs.getBoolean("activo"));
        cupon.setConsumido(rs.getBoolean("consumido"));
        cupon.setEliminado(rs.getBoolean("eliminado"));
        cupon.setUsuarioCreacion(rs.getString("usuario_creacion"));
        cupon.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
        cupon.setUsuarioActualizacion(rs.getString("usuario_actualizacion"));
        cupon.setFechaActualizacion(rs.getObject("fecha_actualizacion", LocalDateTime.class));
        return cupon;
    }

    @Override
    public ResultadoPaginadoDTO<CuponDTO> listarPorUsuario(int idUsuario, LocalDateTime fecha, int pagina, int tamanio) throws SQLException {
        if (pagina < 1 || tamanio < 1) throw new IllegalArgumentException("Pagina y tamanio deben ser positivos.");
        if (fecha == null) throw new IllegalArgumentException("La fecha de consulta es obligatoria.");
        String sql = "SELECT * FROM CUPON WHERE id_usuario = ? AND NOT eliminado ORDER BY fecha_creacion DESC, id DESC LIMIT ? OFFSET ?";
        List<CuponDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement statement = conexion.prepareStatement("SELECT COUNT(*) FROM CUPON WHERE id_usuario = ? AND NOT eliminado")) {
                statement.setInt(1, idUsuario);
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                statement.setInt(1, idUsuario);
                statement.setInt(2, tamanio);
                statement.setLong(3, (long) (pagina - 1) * tamanio);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Cupon cupon = mapearCupon(rs);
                        EstadoCupon estado = cupon.isConsumido() ? EstadoCupon.UTILIZADO :
                                (!cupon.isActivo() || fecha.isBefore(cupon.getFechaInicio()) || fecha.isAfter(cupon.getFechaFin()))
                                        ? EstadoCupon.VENCIDO : EstadoCupon.DISPONIBLE;
                        items.add(new CuponDTO(cupon.getId(), cupon.getPorcentaje(), cupon.getMontoMinimo(), cupon.getFechaFin(), estado));
                    }
                }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, pagina, tamanio);
    }

    @Override
    public void actualizarVigencias(LocalDateTime fecha, String actor) throws SQLException {
        String sql = """
                UPDATE CUPON SET activo = (? BETWEEN fecha_inicio AND fecha_fin),
                    usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE NOT eliminado AND activo <> (? BETWEEN fecha_inicio AND fecha_fin)
                """;
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(fecha));
            statement.setString(2, actor);
            statement.setTimestamp(3, Timestamp.valueOf(fecha));
            statement.executeUpdate();
        }
    }

    @Override
    public boolean consumir(int id, int idUsuario, LocalDateTime fecha, String actor) throws SQLException {
        String sql = """
                UPDATE CUPON SET consumido = TRUE, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ? AND id_usuario = ? AND activo AND NOT eliminado AND NOT consumido
                    AND ? BETWEEN fecha_inicio AND fecha_fin
                """;
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, actor);
            statement.setInt(2, id);
            statement.setInt(3, idUsuario);
            statement.setTimestamp(4, Timestamp.valueOf(fecha));
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean liberar(int id, LocalDateTime fecha, String actor) throws SQLException {
        String sql = """
                UPDATE CUPON c SET consumido = FALSE, activo = TRUE, usuario_actualizacion = ?,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ? AND NOT eliminado AND consumido AND ? BETWEEN fecha_inicio AND fecha_fin
                    AND NOT EXISTS (SELECT 1 FROM PEDIDO p WHERE p.id_cupon = c.id
                        AND p.estado IN ('EN_ALMACEN', 'EN_CAMINO', 'ENTREGADO'))
                """;
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, actor);
            statement.setInt(2, id);
            statement.setTimestamp(3, Timestamp.valueOf(fecha));
            return statement.executeUpdate() == 1;
        }
    }
}
