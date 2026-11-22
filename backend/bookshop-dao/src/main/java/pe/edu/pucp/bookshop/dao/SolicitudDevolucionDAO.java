package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.SolicitudDevolucion;

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
