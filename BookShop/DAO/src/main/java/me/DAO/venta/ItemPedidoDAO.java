package me.DAO.venta;

import java.sql.SQLException;
import java.util.List;
import me.Model.dto.compra.ItemDTO;
import me.Model.entities.compra.Item;

// Las líneas cobradas son históricas: no se editan ni retiran por separado.
public interface ItemPedidoDAO {
    void insertar(int idPedido, Item item) throws SQLException;
    List<Item> listarPorPedido(int idPedido) throws SQLException;
    List<Item> listarParaActualizar(int idPedido) throws SQLException;
    List<ItemDTO> obtenerItemsDTO(int idPedido) throws SQLException;
}
