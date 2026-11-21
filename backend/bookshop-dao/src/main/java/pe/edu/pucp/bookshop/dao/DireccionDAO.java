package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.Direccion;

import java.sql.SQLException;
import java.util.List;

public interface DireccionDAO {
    List<Direccion> listarActivasPorUsuario(int idUsuario) throws SQLException;
    int insertar(Direccion direccion) throws SQLException;
    void modificar(Direccion direccion) throws SQLException;
    void desactivar(int id) throws SQLException;
    // Usan la conexión de TransactionContext
    void quitarPredeterminada(int idUsuario) throws SQLException;
    void marcarPredeterminada(int id) throws SQLException;
}
