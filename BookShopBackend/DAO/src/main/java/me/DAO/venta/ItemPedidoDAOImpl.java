package me.DAO.venta;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, idPedido);
            sentencia.setInt(2, item.getLibro().getId());
            sentencia.setInt(3, item.getCantidad());
            sentencia.setBigDecimal(4, item.getPrecioBase());
            sentencia.setBigDecimal(5, item.getPrecioUnitario());
            sentencia.setBigDecimal(6, item.getPorcentajeDescuento());
            sentencia.setBigDecimal(7, item.getSubTotal());
            sentencia.setString(8, item.getUsuarioCreacion());
            sentencia.setTimestamp(9, Timestamp.valueOf(item.getFechaCreacion()));
            sentencia.setBoolean(10, item.isEliminado());
            sentencia.executeUpdate();
            try (ResultSet resultado = sentencia.getGeneratedKeys()) {
                if (!resultado.next()) throw new SQLException("No se genero el identificador del item.");
                item.setId(resultado.getInt(1));
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idPedido);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    Item item = new Item();
                    item.setId(resultado.getInt("id"));
                    Libro libro = new Libro();
                    libro.setId(resultado.getInt("id_libro"));
                    item.setLibro(libro);
                    item.setCantidad(resultado.getInt("cantidad"));
                    item.setPrecioBase(resultado.getBigDecimal("precio_base"));
                    item.setPrecioUnitario(resultado.getBigDecimal("precio_unitario"));
                    item.setPorcentajeDescuento(resultado.getBigDecimal("porcentaje_descuento"));
                    item.setSubTotal(resultado.getBigDecimal("sub_total"));
                    item.setEliminado(resultado.getBoolean("eliminado"));
                    item.setUsuarioCreacion(resultado.getString("usuario_creacion"));
                    item.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
                    item.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
                    item.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
                    items.add(item);
                }
            }
        }
        return items;
    }

    @Override
    public List<ItemDTO> obtenerItemsDTO(int idPedido) throws SQLException {
        String sql = """
                SELECT i.*, l.titulo, l.portada
                FROM ITEM_PEDIDO i JOIN LIBRO l ON l.id = i.id_libro
                WHERE i.id_pedido = ? AND NOT i.eliminado ORDER BY i.id
                """;
        List<ItemDTO> items = new ArrayList<>();
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idPedido);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    String portada = resultado.getString("portada");
                    BigDecimal original = resultado.getBigDecimal("precio_base");
                    BigDecimal unitario = resultado.getBigDecimal("precio_unitario");
                    items.add(new ItemDTO(resultado.getInt("id_libro"), resultado.getString("titulo"),
                            portada,
                            resultado.getInt("cantidad"), original, unitario, resultado.getBigDecimal("porcentaje_descuento"), resultado.getBigDecimal("sub_total"), null, null));
                }
            }
        }
        return items;
    }
}
