package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.ReporteResena;

import java.sql.SQLException;
import java.util.List;

public interface ReporteResenaDAO {
    // Con la reseña, su libro y su autor
    List<ReporteResena> listarPorUsuario(int idUsuario) throws SQLException;
    // Con la reseña, su libro, su autor y quien reportó
    List<ReporteResena> listarPorEstado(EstadoSolicitud estado) throws SQLException;
    int contarPorUsuarioYResena(int idUsuario, int idResena, EstadoSolicitud estado) throws SQLException;
    int insertar(ReporteResena reporte) throws SQLException;
    void modificar(ReporteResena reporte) throws SQLException;
    // Usan la conexión de TransactionContext
    void cambiarEstado(int id, EstadoSolicitud estado) throws SQLException;
    void eliminarPorResena(int idResena) throws SQLException;
}
