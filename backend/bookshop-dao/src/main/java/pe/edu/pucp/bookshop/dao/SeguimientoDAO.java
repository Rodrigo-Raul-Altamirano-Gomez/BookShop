package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.SeguimientoEntidad;
import pe.edu.pucp.bookshop.model.TipoEntidad;

import java.sql.SQLException;
import java.util.List;

public interface SeguimientoDAO {
    // Con la entidad completa (también si está inactiva)
    List<SeguimientoEntidad> listarPorUsuario(int idUsuario, TipoEntidad tipo) throws SQLException;
    boolean existe(int idUsuario, int idEntidad) throws SQLException;
    void insertar(SeguimientoEntidad seguimiento) throws SQLException;
    void eliminar(int idUsuario, int idEntidad) throws SQLException;
}
