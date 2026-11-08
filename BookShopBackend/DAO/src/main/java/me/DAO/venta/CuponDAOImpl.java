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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, cupon.getUsuario().getId());
            sentencia.setBigDecimal(2, cupon.getPorcentaje());
            sentencia.setBigDecimal(3, cupon.getMontoMinimo());
            sentencia.setTimestamp(4, Timestamp.valueOf(cupon.getFechaInicio()));
            sentencia.setTimestamp(5, Timestamp.valueOf(cupon.getFechaFin()));
            sentencia.setBoolean(6, cupon.isActivo());
            sentencia.setBoolean(7, cupon.isConsumido());
            sentencia.setBoolean(8, cupon.isEliminado());
            sentencia.setString(9, cupon.getUsuarioCreacion());
            sentencia.setTimestamp(10, Timestamp.valueOf(cupon.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet resultado = sentencia.getGeneratedKeys()) {
                if (!resultado.next()) throw new SQLException("No se genero el identificador del cupon.");
                cupon.setId(resultado.getInt(1));
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapearCupon(resultado) : null;
            }
        }
    }

    private Cupon mapearCupon(ResultSet resultado) throws SQLException {
        Cupon cupon = new Cupon();
        cupon.setId(resultado.getInt("id"));
        Usuario usuario = new Usuario();
        usuario.setId(resultado.getInt("id_usuario"));
        cupon.setUsuario(usuario);
        cupon.setPorcentaje(resultado.getBigDecimal("porcentaje"));
        cupon.setMontoMinimo(resultado.getBigDecimal("monto_minimo"));
        cupon.setFechaInicio(resultado.getObject("fecha_inicio", LocalDateTime.class));
        cupon.setFechaFin(resultado.getObject("fecha_fin", LocalDateTime.class));
        cupon.setActivo(resultado.getBoolean("activo"));
        cupon.setConsumido(resultado.getBoolean("consumido"));
        cupon.setEliminado(resultado.getBoolean("eliminado"));
        cupon.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        cupon.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
        cupon.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        cupon.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
        return cupon;
    }

    @Override
    public ResultadoPaginadoDTO<CuponDTO> listarPorUsuario(int idUsuario, LocalDateTime fecha, int pagina, int tamanioPagina) throws SQLException {
        String sql = "SELECT * FROM CUPON WHERE id_usuario = ? AND NOT eliminado ORDER BY fecha_creacion DESC, id DESC LIMIT ? OFFSET ?";
        List<CuponDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM CUPON WHERE id_usuario = ? AND NOT eliminado")) {
                sentencia.setInt(1, idUsuario);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUsuario);
                sentencia.setInt(2, tamanioPagina);
                sentencia.setLong(3, (long) (pagina - 1) * tamanioPagina);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        Cupon cupon = mapearCupon(resultado);
                        EstadoCupon estado = cupon.isConsumido() ? EstadoCupon.UTILIZADO :
                                (!cupon.isActivo() || fecha.isBefore(cupon.getFechaInicio()) || fecha.isAfter(cupon.getFechaFin()))
                                        ? EstadoCupon.VENCIDO : EstadoCupon.DISPONIBLE;
                        items.add(new CuponDTO(cupon.getId(), cupon.getPorcentaje(), cupon.getMontoMinimo(), cupon.getFechaFin(), estado));
                    }
                }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, pagina, tamanioPagina);
    }

    @Override
    public void actualizarVigencias(LocalDateTime fecha, String actor) throws SQLException {
        String sql = """
                UPDATE CUPON SET activo = (? BETWEEN fecha_inicio AND fecha_fin),
                    usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE NOT eliminado AND activo <> (? BETWEEN fecha_inicio AND fecha_fin)
                """;
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTimestamp(1, Timestamp.valueOf(fecha));
            sentencia.setString(2, actor);
            sentencia.setTimestamp(3, Timestamp.valueOf(fecha));
            sentencia.executeUpdate();
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, actor);
            sentencia.setInt(2, id);
            sentencia.setInt(3, idUsuario);
            sentencia.setTimestamp(4, Timestamp.valueOf(fecha));
            return sentencia.executeUpdate() == 1;
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, actor);
            sentencia.setInt(2, id);
            sentencia.setTimestamp(3, Timestamp.valueOf(fecha));
            return sentencia.executeUpdate() == 1;
        }
    }
}
