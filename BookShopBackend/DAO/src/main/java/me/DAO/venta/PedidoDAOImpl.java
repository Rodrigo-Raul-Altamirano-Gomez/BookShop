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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, pedido.getUsuario().getId());
            sentencia.setBigDecimal(2, pedido.getSubtotalProductos());
            sentencia.setBigDecimal(3, pedido.getDescuentoLibros());
            if (pedido.getCupon() == null) sentencia.setNull(4, Types.INTEGER);
            else sentencia.setInt(4, pedido.getCupon().getId());
            sentencia.setBigDecimal(5, pedido.getPorcentajeCupon());
            sentencia.setBigDecimal(6, pedido.getDescuentoCupon());
            sentencia.setBigDecimal(7, pedido.getPorcentajeEnvio());
            sentencia.setBigDecimal(8, pedido.getCostoEnvio());
            sentencia.setBigDecimal(9, pedido.getTotal());
            sentencia.setInt(10, pedido.getCantidadTotalLibros());
            sentencia.setInt(11, pedido.getDireccionEnvio().getId());
            sentencia.setInt(12, pedido.getMetodoPago().getId());
            sentencia.setString(13, pedido.getIdentificadorTransaccion());
            sentencia.setTimestamp(14, Timestamp.valueOf(pedido.getFechaPago()));
            sentencia.setString(15, pedido.getEstado().name());
            sentencia.setString(16, pedido.getCodigoSeguimiento());
            sentencia.setTimestamp(17, Timestamp.valueOf(pedido.getFechaEntregaEstimada()));
            if (pedido.getFechaSalida() == null) sentencia.setNull(18, Types.TIMESTAMP);
            else sentencia.setTimestamp(18, Timestamp.valueOf(pedido.getFechaSalida()));
            if (pedido.getFechaEntrega() == null) sentencia.setNull(19, Types.TIMESTAMP);
            else sentencia.setTimestamp(19, Timestamp.valueOf(pedido.getFechaEntrega()));
            sentencia.setInt(20, pedido.getNumeroIntentos());
            sentencia.setBoolean(21, pedido.isActivo());
            sentencia.setBoolean(22, pedido.isEliminado());
            sentencia.setString(23, pedido.getUsuarioCreacion());
            sentencia.setTimestamp(24, Timestamp.valueOf(pedido.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet resultado = sentencia.getGeneratedKeys()) {
                if (!resultado.next()) throw new SQLException("No se genero el identificador del pedido.");
                pedido.setId(resultado.getInt(1));
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next() ? mapearPedido(resultado) : null; }
        }
    }

    private Pedido mapearPedido(ResultSet resultado) throws SQLException {
        Pedido pedido = new Pedido();
        pedido.setId(resultado.getInt("id"));
        Usuario usuario = new Usuario();
        usuario.setId(resultado.getInt("id_usuario"));
        pedido.setUsuario(usuario);
        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setId(resultado.getInt("id_direccion_envio"));
        pedido.setDireccionEnvio(direccion);
        MetodoPago metodo = "BILLETERA_DIGITAL".equals(resultado.getString("tipo_metodo")) ? new BilleteraDigital() : new TarjetaBancaria();
        metodo.setId(resultado.getInt("id_metodo_pago"));
        pedido.setMetodoPago(metodo);
        int idCupon = resultado.getInt("id_cupon");
        if (!resultado.wasNull()) {
            Cupon cupon = new Cupon();
            cupon.setId(idCupon);
            pedido.setCupon(cupon);
        }
        pedido.setSubtotalProductos(resultado.getBigDecimal("subtotal_productos"));
        pedido.setDescuentoLibros(resultado.getBigDecimal("descuento_libros"));
        pedido.setPorcentajeCupon(resultado.getBigDecimal("porcentaje_cupon"));
        pedido.setDescuentoCupon(resultado.getBigDecimal("descuento_cupon"));
        pedido.setPorcentajeEnvio(resultado.getBigDecimal("porcentaje_envio"));
        pedido.setCostoEnvio(resultado.getBigDecimal("costo_envio"));
        pedido.setTotal(resultado.getBigDecimal("total"));
        pedido.setCantidadTotalLibros(resultado.getInt("cantidad_total_libros"));
        pedido.setIdentificadorTransaccion(resultado.getString("identificador_transaccion"));
        pedido.setFechaPago(resultado.getObject("fecha_pago", LocalDateTime.class));
        pedido.setEstado(EstadoPedido.valueOf(resultado.getString("estado")));
        pedido.setCodigoSeguimiento(resultado.getString("codigo_seguimiento"));
        pedido.setFechaEntregaEstimada(resultado.getObject("fecha_entrega_estimada", LocalDateTime.class));
        pedido.setFechaSalida(resultado.getObject("fecha_salida", LocalDateTime.class));
        pedido.setFechaEntrega(resultado.getObject("fecha_entrega", LocalDateTime.class));
        pedido.setNumeroIntentos(resultado.getInt("numero_intentos"));
        pedido.setActivo(resultado.getBoolean("activo"));
        pedido.setEliminado(resultado.getBoolean("eliminado"));
        pedido.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        pedido.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
        pedido.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        pedido.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
        return pedido;
    }

    @Override
    public ResumenPedidoDTO obtenerResumen(int id) throws SQLException {
        String sql = "SELECT id, fecha_pago, estado, cantidad_total_libros, total FROM PEDIDO WHERE id = ?";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next() ? mapearResumen(resultado) : null; }
        }
    }

    private ResumenPedidoDTO mapearResumen(ResultSet resultado) throws SQLException {
        return new ResumenPedidoDTO(resultado.getInt("id"), resultado.getObject("fecha_pago", LocalDateTime.class),
                EstadoPedido.valueOf(resultado.getString("estado")), resultado.getInt("cantidad_total_libros"), resultado.getBigDecimal("total"));
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaPedidoDTO> listarPorUsuario(int idUsuario, FiltroPedidoDTO filtro) throws SQLException {
        if (filtro == null) filtro = FiltroPedidoDTO.porDefecto();
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
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM PEDIDO" + condiciones)) {
                sentencia.setInt(1, idUsuario);
                int parametro = 2;
                for (EstadoPedido estado : filtro.estados()) sentencia.setString(parametro++, estado.name());
                try (ResultSet resultado = sentencia.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT id, fecha_pago, estado, cantidad_total_libros, total FROM PEDIDO" + condiciones + " ORDER BY " + ordenar + " LIMIT ? OFFSET ?")) {
                sentencia.setInt(1, idUsuario);
                int parametro = 2;
                for (EstadoPedido estado : filtro.estados()) sentencia.setString(parametro++, estado.name());
                sentencia.setInt(parametro++, filtro.tamanioPagina());
                sentencia.setLong(parametro, (long) (filtro.pagina() - 1) * filtro.tamanioPagina());
                try (ResultSet resultado = sentencia.executeQuery()) { while (resultado.next()) pedidos.add(mapearResumen(resultado)); }
            }
            // Una consulta conjunta recupera solo las portadas de esta página.
            if (!pedidos.isEmpty()) {
                String sql = "SELECT i.id_pedido, i.id_libro, l.portada, l.tipo_portada FROM ITEM_PEDIDO i JOIN LIBRO l ON l.id = i.id_libro"
                        + " WHERE NOT i.eliminado AND i.id_pedido IN (" + String.join(",", java.util.Collections.nCopies(pedidos.size(), "?")) + ") ORDER BY i.id_pedido, i.id";
                try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                    int parametro = 1;
                    for (ResumenPedidoDTO pedido : pedidos) sentencia.setInt(parametro++, pedido.id());
                    try (ResultSet resultado = sentencia.executeQuery()) {
                        while (resultado.next()) {
                            int idPedido = resultado.getInt("id_pedido");
                            List<ImagenDTO> imagenesPedido = portadas.get(idPedido);
                            if (imagenesPedido == null) {
                                imagenesPedido = new ArrayList<>();
                                portadas.put(idPedido, imagenesPedido);
                            }
                            imagenesPedido.add(new ImagenDTO(resultado.getBytes("portada"), resultado.getString("tipo_portada")));
                        }
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, pedido.getEstado().name());
            if (pedido.getFechaSalida() == null) sentencia.setNull(2, Types.TIMESTAMP);
            else sentencia.setTimestamp(2, Timestamp.valueOf(pedido.getFechaSalida()));
            if (pedido.getFechaEntrega() == null) sentencia.setNull(3, Types.TIMESTAMP);
            else sentencia.setTimestamp(3, Timestamp.valueOf(pedido.getFechaEntrega()));
            sentencia.setString(4, pedido.getUsuarioActualizacion());
            sentencia.setTimestamp(5, Timestamp.valueOf(pedido.getFechaActualizacion() == null ? LocalDateTime.now() : pedido.getFechaActualizacion()));
            sentencia.setInt(6, pedido.getId());
            sentencia.setString(7, estadoAnterior.name());
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean incrementarIntentos(int id, int maximoIntentos, String actor) throws SQLException {
        String sql = """
                UPDATE PEDIDO p SET numero_intentos = numero_intentos + 1,
                    usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'ENTREGADO' AND NOT eliminado AND numero_intentos < ?
                    AND NOT EXISTS (SELECT 1 FROM SOLICITUD s WHERE s.id_pedido = p.id
                        AND s.tipo_solicitud = 'DEVOLUCION' AND s.estado = 'PENDIENTE' AND NOT s.eliminado)
                """;
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, actor);
            sentencia.setInt(2, id);
            sentencia.setInt(3, maximoIntentos);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean haCompradoLibro(int idUsuario, int idLibro) throws SQLException {
        String sql = """
                SELECT 1 FROM PEDIDO p JOIN ITEM_PEDIDO i ON i.id_pedido = p.id
                WHERE p.id_usuario = ? AND i.id_libro = ? AND NOT p.eliminado AND NOT i.eliminado
                    AND p.estado IN ('EN_ALMACEN', 'EN_CAMINO', 'ENTREGADO') LIMIT 1
                """;
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, idLibro);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next(); }
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
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (desde != null) sentencia.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) items.add(new VentaLibroDTO(resultado.getInt("id"), resultado.getInt("id_usuario"), resultado.getInt("id_libro"), resultado.getInt("cantidad"), resultado.getObject("fecha_pago", LocalDateTime.class)));
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
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, idLibro);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next(); }
        }
    }
}
