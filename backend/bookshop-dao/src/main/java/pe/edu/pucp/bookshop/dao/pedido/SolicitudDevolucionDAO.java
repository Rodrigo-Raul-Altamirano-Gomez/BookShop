package pe.edu.pucp.bookshop.dao.pedido;

import pe.edu.pucp.bookshop.model.comun.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.pedido.SolicitudDevolucion;

import java.sql.SQLException;
import java.util.List;

public interface SolicitudDevolucionDAO {
    // Con el pedido y sus libros (para las portadas)
    List<SolicitudDevolucion> listarPorUsuario(int idUsuario) throws SQLException;
    // Con el pedido, su cliente y sus libros
    List<SolicitudDevolucion> listarPorEstado(EstadoSolicitud estado) throws SQLException;
    List<SolicitudDevolucion> listarPorPedido(int idPedido) throws SQLException;
    int insertar(SolicitudDevolucion solicitud) throws SQLException;
    void modificar(SolicitudDevolucion solicitud) throws SQLException;
    // Usa la conexión de TransactionContext
    void cambiarEstado(int id, EstadoSolicitud estado) throws SQLException;
}
