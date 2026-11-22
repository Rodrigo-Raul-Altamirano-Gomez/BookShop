package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.DetallePedido;
import pe.edu.pucp.bookshop.model.EstadoPedido;
import pe.edu.pucp.bookshop.model.Pedido;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoDAO {
    Pedido obtener(int id) throws SQLException;
    List<DetallePedido> listarDetalles(int idPedido) throws SQLException;
    // Cada pedido con sus libros (para las portadas y las unidades)
    List<Pedido> listarPorUsuario(int idUsuario) throws SQLException;
    List<Pedido> listarPorEstado(EstadoPedido estado) throws SQLException;
    int contarCancelaciones(int idUsuario, LocalDateTime desde) throws SQLException;
    void registrarSalida(int id, LocalDateTime fecha) throws SQLException;
    void registrarEntrega(int id, LocalDateTime fecha) throws SQLException;
    // Usan la conexión de TransactionContext
    int insertar(Pedido pedido) throws SQLException;
    void insertarDetalle(int idPedido, DetallePedido detalle) throws SQLException;
    void cancelar(int id, LocalDateTime fecha) throws SQLException;
    void registrarDevolucion(int id, LocalDateTime fecha) throws SQLException;
    void marcarContadoParaCupon(int id) throws SQLException;
    int contarContadosParaCupon(int idUsuario) throws SQLException;
}
