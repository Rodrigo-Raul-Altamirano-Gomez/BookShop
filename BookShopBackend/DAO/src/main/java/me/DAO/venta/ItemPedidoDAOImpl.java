package me.DAO.venta;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.compra.ItemDTO;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.compra.Item;

public class ItemPedidoDAOImpl implements ItemPedidoDAO {
    @Override
    public void insertar(int idPedido, Item item) throws SQLException {
        String sql = """
                INSERT INTO ITEM_PEDIDO (id_pedido, id_libro, cantidad, precio_base, precio_unitario,
                    porcentaje_descuento, sub_total, usuario_creacion, fecha_creacion, eliminado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, idPedido);
            statement.setInt(2, item.getLibro().getId());
            statement.setInt(3, item.getCantidad());
            statement.setBigDecimal(4, item.getPrecioBase());
            statement.setBigDecimal(5, item.getPrecioUnitario());
            statement.setBigDecimal(6, item.getPorcentajeDescuento());
            statement.setBigDecimal(7, item.getSubTotal());
            statement.setString(8, item.getUsuarioCreacion());
            statement.setTimestamp(9, Timestamp.valueOf(item.getFechaCreacion()));
            statement.setBoolean(10, item.isEliminado());
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("No se genero el identificador del item.");
                item.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public List<Item> listarPorPedido(int idPedido) throws SQLException {
        try (Connection conexion = DBManager.getConnection()) { return listar(conexion, idPedido, false); }
    }

    @Override
    public List<Item> listarParaActualizar(int idPedido) throws SQLException {
        return listar(TransactionContext.getConnection(), idPedido, true);
    }

    private List<Item> listar(Connection conexion, int idPedido, boolean bloquear) throws SQLException {
        String sql = "SELECT * FROM ITEM_PEDIDO WHERE id_pedido = ? AND NOT eliminado ORDER BY id" + (bloquear ? " FOR UPDATE" : "");
        List<Item> items = new ArrayList<>();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idPedido);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Item item = new Item();
                    item.setId(rs.getInt("id"));
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    item.setLibro(libro);
                    item.setCantidad(rs.getInt("cantidad"));
                    item.setPrecioBase(rs.getBigDecimal("precio_base"));
                    item.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    item.setPorcentajeDescuento(rs.getBigDecimal("porcentaje_descuento"));
                    item.setSubTotal(rs.getBigDecimal("sub_total"));
                    item.setEliminado(rs.getBoolean("eliminado"));
                    item.setUsuarioCreacion(rs.getString("usuario_creacion"));
                    item.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
                    item.setUsuarioActualizacion(rs.getString("usuario_actualizacion"));
                    item.setFechaActualizacion(rs.getObject("fecha_actualizacion", LocalDateTime.class));
                    items.add(item);
                }
            }
        }
        return items;
    }

    @Override
    public List<ItemDTO> obtenerItemsDTO(int idPedido) throws SQLException {
        String sql = """
                SELECT i.*, l.titulo, l.portada, l.tipo_portada
                FROM ITEM_PEDIDO i JOIN LIBRO l ON l.id = i.id_libro
                WHERE i.id_pedido = ? AND NOT i.eliminado ORDER BY i.id
                """;
        List<ItemDTO> items = new ArrayList<>();
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idPedido);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    byte[] portada = rs.getBytes("portada");
                    BigDecimal original = rs.getBigDecimal("precio_base");
                    BigDecimal unitario = rs.getBigDecimal("precio_unitario");
                    items.add(new ItemDTO(rs.getInt("id_libro"), rs.getString("titulo"),
                            portada == null ? new ImagenDTO(null, null) : new ImagenDTO(portada, rs.getString("tipo_portada")),
                            rs.getInt("cantidad"), original, unitario, rs.getBigDecimal("porcentaje_descuento"), rs.getBigDecimal("sub_total"), null, null));
                }
            }
        }
        return items;
    }
}

