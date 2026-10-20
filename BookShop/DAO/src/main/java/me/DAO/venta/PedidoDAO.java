package me.DAO.venta;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.*;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.compra.Pedido;

public interface PedidoDAO {
    void insertar(Pedido pedido) throws SQLException;
    Pedido leer(int id) throws SQLException;
    Pedido leerParaActualizar(int id) throws SQLException;
    ResumenPedidoDTO obtenerResumen(int id) throws SQLException;
    ResultadoPaginadoDTO<TarjetaPedidoDTO> listarPorUsuario(int idUsuario, FiltroPedidoDTO filtro) throws SQLException;
    List<ItemDTO> obtenerItemsDTO(int idPedido) throws SQLException;
    boolean actualizarEstado(Pedido pedido, EstadoPedido estadoAnterior) throws SQLException;
    boolean incrementarIntentos(int id, int maximoIntentos, String actor) throws SQLException;
    boolean haCompradoLibro(int idUsuario, int idLibro) throws SQLException;
    boolean haCompradoLibroParaActualizar(int idUsuario, int idLibro) throws SQLException;
    List<VentaLibroDTO> listarDetalleVentas(LocalDate desde) throws SQLException;
}
