package me.DAO.venta;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.*;
import me.Model.entities.compra.*;
import me.Model.entities.usuario.Usuario;

public class PedidoDAOImpl implements PedidoDAO {
    @Override
    public void insertar(Pedido pedido) throws SQLException {
        if (pedido.getEstado() == null || pedido.getEstado() == EstadoPedido.EN_ESPERA)
            throw new IllegalArgumentException("Un pedido en espera solo existe en memoria.");
        String sql = """
                INSERT INTO PEDIDO (id_usuario, subtotal_productos, descuento_libros, id_cupon,
                    porcentaje_cupon, descuento_cupon, porcentaje_envio, costo_envio, total,
                    cantidad_total_libros, id_direccion_envio, id_metodo_pago, identificador_transaccion,
                    fecha_pago, estado, codigo_seguimiento, fecha_entrega_estimada, fecha_salida,
                    fecha_entrega, numero_intentos, activo, eliminado, usuario_creacion, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, pedido.getUsuario().getId());
            statement.setBigDecimal(2, pedido.getSubtotalProductos());
            statement.setBigDecimal(3, pedido.getDescuentoLibros());
            if (pedido.getCupon() == null) statement.setNull(4, Types.INTEGER);
            else statement.setInt(4, pedido.getCupon().getId());
            statement.setBigDecimal(5, pedido.getPorcentajeCupon());
            statement.setBigDecimal(6, pedido.getDescuentoCupon());
            statement.setBigDecimal(7, pedido.getPorcentajeEnvio());
            statement.setBigDecimal(8, pedido.getCostoEnvio());
            statement.setBigDecimal(9, pedido.getTotal());
            statement.setInt(10, pedido.getCantidadTotalLibros());
            statement.setInt(11, pedido.getDireccionEnvio().getId());
            statement.setInt(12, pedido.getMetodoPago().getId());
            statement.setString(13, pedido.getIdentificadorTransaccion());
            statement.setTimestamp(14, Timestamp.valueOf(pedido.getFechaPago()));
            statement.setString(15, pedido.getEstado().name());
            statement.setString(16, pedido.getCodigoSeguimiento());
            statement.setTimestamp(17, Timestamp.valueOf(pedido.getFechaEntregaEstimada()));
            if (pedido.getFechaSalida() == null) statement.setNull(18, Types.TIMESTAMP);
            else statement.setTimestamp(18, Timestamp.valueOf(pedido.getFechaSalida()));
            if (pedido.getFechaEntrega() == null) statement.setNull(19, Types.TIMESTAMP);
            else statement.setTimestamp(19, Timestamp.valueOf(pedido.getFechaEntrega()));
            statement.setInt(20, pedido.getNumeroIntentos());
            statement.setBoolean(21, pedido.isActivo());
            statement.setBoolean(22, pedido.isEliminado());
            statement.setString(23, pedido.getUsuarioCreacion());
            statement.setTimestamp(24, Timestamp.valueOf(pedido.getFechaCreacion()));
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("No se genero el identificador del pedido.");
                pedido.setId(rs.getInt(1));
            }
        }
        // Cabecera y líneas comparten la compra. Business confirma todos sus efectos juntos.
        ItemPedidoDAO items = new ItemPedidoDAOImpl();
        for (Item item : pedido.getItems()) {
            if (item.getUsuarioCreacion() == null) item.setUsuarioCreacion(pedido.getUsuarioCreacion());
            item.setFechaCreacion(pedido.getFechaCreacion());
            items.insertar(pedido.getId(), item);
        }
    }

    @Override
    public Pedido leer(int id) throws SQLException {
        Pedido pedido;
        try (Connection conexion = DBManager.getConnection()) { pedido = leerRegistro(conexion, id, false); }
        if (pedido != null) pedido.setItems(new ItemPedidoDAOImpl().listarPorPedido(id));
        return pedido;
    }

    @Override
    public Pedido leerParaActualizar(int id) throws SQLException {
        Pedido pedido = leerRegistro(TransactionContext.getConnection(), id, true);
        if (pedido != null) pedido.setItems(new ItemPedidoDAOImpl().listarParaActualizar(id));
        return pedido;
    }

    private Pedido leerRegistro(Connection conexion, int id, boolean bloquear) throws SQLException {
        String sql = "SELECT p.*, m.tipo_metodo FROM PEDIDO p JOIN METODO_PAGO m ON m.id = p.id_metodo_pago WHERE p.id = ?"
                + (bloquear ? " FOR UPDATE" : "");
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? mapearPedido(rs) : null; }
        }
    }

    private Pedido mapearPedido(ResultSet rs) throws SQLException {
        Pedido pedido = new Pedido();
        pedido.setId(rs.getInt("id"));
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario"));
        pedido.setUsuario(usuario);
        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setId(rs.getInt("id_direccion_envio"));
        pedido.setDireccionEnvio(direccion);
        MetodoPago metodo = "BILLETERA_DIGITAL".equals(rs.getString("tipo_metodo")) ? new BilleteraDigital() : new TarjetaBancaria();
        metodo.setId(rs.getInt("id_metodo_pago"));
        pedido.setMetodoPago(metodo);
        int idCupon = rs.getInt("id_cupon");
        if (!rs.wasNull()) {
            Cupon cupon = new Cupon();
            cupon.setId(idCupon);
            pedido.setCupon(cupon);
        }
        pedido.setSubtotalProductos(rs.getBigDecimal("subtotal_productos"));
        pedido.setDescuentoLibros(rs.getBigDecimal("descuento_libros"));
        pedido.setPorcentajeCupon(rs.getBigDecimal("porcentaje_cupon"));
        pedido.setDescuentoCupon(rs.getBigDecimal("descuento_cupon"));
        pedido.setPorcentajeEnvio(rs.getBigDecimal("porcentaje_envio"));
        pedido.setCostoEnvio(rs.getBigDecimal("costo_envio"));
        pedido.setTotal(rs.getBigDecimal("total"));
        pedido.setCantidadTotalLibros(rs.getInt("cantidad_total_libros"));
        pedido.setIdentificadorTransaccion(rs.getString("identificador_transaccion"));
        pedido.setFechaPago(rs.getObject("fecha_pago", LocalDateTime.class));
        pedido.setEstado(EstadoPedido.valueOf(rs.getString("estado")));
        pedido.setCodigoSeguimiento(rs.getString("codigo_seguimiento"));
        pedido.setFechaEntregaEstimada(rs.getObject("fecha_entrega_estimada", LocalDateTime.class));
        pedido.setFechaSalida(rs.getObject("fecha_salida", LocalDateTime.class));
        pedido.setFechaEntrega(rs.getObject("fecha_entrega", LocalDateTime.class));
        pedido.setNumeroIntentos(rs.getInt("numero_intentos"));
        pedido.setActivo(rs.getBoolean("activo"));
        pedido.setEliminado(rs.getBoolean("eliminado"));
        pedido.setUsuarioCreacion(rs.getString("usuario_creacion"));
        pedido.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
        pedido.setUsuarioActualizacion(rs.getString("usuario_actualizacion"));
        pedido.setFechaActualizacion(rs.getObject("fecha_actualizacion", LocalDateTime.class));
        return pedido;
    }

    @Override
    public ResumenPedidoDTO obtenerResumen(int id) throws SQLException {
        String sql = "SELECT id, fecha_pago, estado, cantidad_total_libros, total FROM PEDIDO WHERE id = ?";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? mapearResumen(rs) : null; }
        }
    }

    private ResumenPedidoDTO mapearResumen(ResultSet rs) throws SQLException {
        return new ResumenPedidoDTO(rs.getInt("id"), rs.getObject("fecha_pago", LocalDateTime.class),
                EstadoPedido.valueOf(rs.getString("estado")), rs.getInt("cantidad_total_libros"), rs.getBigDecimal("total"));
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaPedidoDTO> listarPorUsuario(int idUsuario, FiltroPedidoDTO filtro) throws SQLException {
        if (filtro == null) filtro = new FiltroPedidoDTO();
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1) throw new IllegalArgumentException("Pagina y tamanio deben ser positivos.");
        if (filtro.estados().contains(EstadoPedido.EN_ESPERA)) throw new IllegalArgumentException("En espera no forma parte del historial.");
        String condiciones = " WHERE id_usuario = ? AND NOT eliminado";
        if (!filtro.estados().isEmpty()) condiciones += " AND estado IN (" + String.join(",", java.util.Collections.nCopies(filtro.estados().size(), "?")) + ")";
        OrdenPedido orden = filtro.orden() == null ? OrdenPedido.FECHA_PAGO_DESCENDENTE : filtro.orden();
        String ordenar = switch (orden) {
            case TOTAL_ASCENDENTE -> "total ASC, fecha_pago DESC, id DESC";
            case TOTAL_DESCENDENTE -> "total DESC, fecha_pago DESC, id DESC";
            case FECHA_PAGO_DESCENDENTE -> "fecha_pago DESC, id DESC";
        };
        List<ResumenPedidoDTO> pedidos = new ArrayList<>();
        Map<Integer, List<ImagenDTO>> portadas = new HashMap<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement statement = conexion.prepareStatement("SELECT COUNT(*) FROM PEDIDO" + condiciones)) {
                statement.setInt(1, idUsuario);
                int parametro = 2;
                for (EstadoPedido estado : filtro.estados()) statement.setString(parametro++, estado.name());
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            try (PreparedStatement statement = conexion.prepareStatement("SELECT id, fecha_pago, estado, cantidad_total_libros, total FROM PEDIDO" + condiciones + " ORDER BY " + ordenar + " LIMIT ? OFFSET ?")) {
                statement.setInt(1, idUsuario);
                int parametro = 2;
                for (EstadoPedido estado : filtro.estados()) statement.setString(parametro++, estado.name());
                statement.setInt(parametro++, filtro.tamanioPagina());
                statement.setLong(parametro, (long) (filtro.pagina() - 1) * filtro.tamanioPagina());
                try (ResultSet rs = statement.executeQuery()) { while (rs.next()) pedidos.add(mapearResumen(rs)); }
            }
            // Una consulta conjunta recupera solo las portadas de esta página.
            if (!pedidos.isEmpty()) {
                String sql = "SELECT i.id_pedido, i.id_libro, l.portada, l.tipo_portada FROM ITEM_PEDIDO i JOIN LIBRO l ON l.id = i.id_libro"
                        + " WHERE NOT i.eliminado AND i.id_pedido IN (" + String.join(",", java.util.Collections.nCopies(pedidos.size(), "?")) + ") ORDER BY i.id_pedido, i.id";
                try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                    int parametro = 1;
                    for (ResumenPedidoDTO pedido : pedidos) statement.setInt(parametro++, pedido.id());
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) portadas.computeIfAbsent(rs.getInt("id_pedido"), id -> new ArrayList<>())
                                .add(new ImagenDTO(rs.getBytes("portada"), rs.getString("tipo_portada")));
                    }
                }
            }
        }
        List<TarjetaPedidoDTO> items = new ArrayList<>();
        for (ResumenPedidoDTO pedido : pedidos) items.add(new TarjetaPedidoDTO(pedido, portadas.getOrDefault(pedido.id(), List.of())));
        return new ResultadoPaginadoDTO<>(items, total, filtro.pagina(), filtro.tamanioPagina());
    }

    @Override
    public List<ItemDTO> obtenerItemsDTO(int idPedido) throws SQLException {
        return new ItemPedidoDAOImpl().obtenerItemsDTO(idPedido);
    }

    @Override
    public boolean actualizarEstado(Pedido pedido, EstadoPedido estadoAnterior) throws SQLException {
        if (pedido.getEstado() == null || pedido.getEstado() == EstadoPedido.EN_ESPERA || estadoAnterior == null || estadoAnterior == EstadoPedido.EN_ESPERA)
            throw new IllegalArgumentException("La transicion requiere estados de un pedido confirmado.");
        if (pedido.getEstado() == estadoAnterior) return false;
        String sql = """
                UPDATE PEDIDO SET estado = ?, fecha_salida = ?, fecha_entrega = ?,
                    usuario_actualizacion = ?, fecha_actualizacion = ?
                WHERE id = ? AND estado = ? AND NOT eliminado
                """;
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, pedido.getEstado().name());
            if (pedido.getFechaSalida() == null) statement.setNull(2, Types.TIMESTAMP);
            else statement.setTimestamp(2, Timestamp.valueOf(pedido.getFechaSalida()));
            if (pedido.getFechaEntrega() == null) statement.setNull(3, Types.TIMESTAMP);
            else statement.setTimestamp(3, Timestamp.valueOf(pedido.getFechaEntrega()));
            statement.setString(4, pedido.getUsuarioActualizacion());
            statement.setTimestamp(5, Timestamp.valueOf(pedido.getFechaActualizacion() == null ? LocalDateTime.now() : pedido.getFechaActualizacion()));
            statement.setInt(6, pedido.getId());
            statement.setString(7, estadoAnterior.name());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean incrementarIntentos(int id, int maximoIntentos, String actor) throws SQLException {
        if (maximoIntentos < 1) throw new IllegalArgumentException("El maximo de intentos debe ser positivo.");
        String sql = """
                UPDATE PEDIDO p SET numero_intentos = numero_intentos + 1,
                    usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'ENTREGADO' AND NOT eliminado AND numero_intentos < ?
                    AND NOT EXISTS (SELECT 1 FROM SOLICITUD s WHERE s.id_pedido = p.id
                        AND s.tipo_solicitud = 'DEVOLUCION' AND s.estado = 'PENDIENTE' AND NOT s.eliminado)
                """;
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, actor);
            statement.setInt(2, id);
            statement.setInt(3, maximoIntentos);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean haCompradoLibro(int idUsuario, int idLibro) throws SQLException {
        String sql = """
                SELECT 1 FROM PEDIDO p JOIN ITEM_PEDIDO i ON i.id_pedido = p.id
                WHERE p.id_usuario = ? AND i.id_libro = ? AND NOT p.eliminado AND NOT i.eliminado
                    AND p.estado IN ('EN_ALMACEN', 'EN_CAMINO', 'ENTREGADO') LIMIT 1
                """;
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idLibro);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public List<VentaLibroDTO> listarDetalleVentas(LocalDate desde) throws SQLException {
        String sql = """
                SELECT p.id, p.id_usuario, i.id_libro, i.cantidad, p.fecha_pago
                FROM PEDIDO p JOIN ITEM_PEDIDO i ON i.id_pedido = p.id
                WHERE NOT p.eliminado AND NOT i.eliminado AND p.estado IN ('EN_ALMACEN', 'EN_CAMINO', 'ENTREGADO')
                """ + (desde == null ? "" : " AND p.fecha_pago >= ?") + " ORDER BY p.fecha_pago, p.id, i.id";
        List<VentaLibroDTO> items = new ArrayList<>();
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            if (desde != null) statement.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) items.add(new VentaLibroDTO(rs.getInt("id"), rs.getInt("id_usuario"), rs.getInt("id_libro"), rs.getInt("cantidad"), rs.getObject("fecha_pago", LocalDateTime.class)));
            }
        }
        return items;
    }

    @Override
    public boolean haCompradoLibroParaActualizar(int idUsuario, int idLibro) throws SQLException {
        String sql = """
                SELECT p.id FROM PEDIDO p JOIN ITEM_PEDIDO i ON i.id_pedido = p.id
                WHERE p.id_usuario = ? AND i.id_libro = ? AND NOT p.eliminado AND NOT i.eliminado
                    AND p.estado IN ('EN_ALMACEN', 'EN_CAMINO', 'ENTREGADO')
                ORDER BY p.id FOR UPDATE
                """;
        var conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idLibro);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }
}
