package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.Cupon;

import java.sql.SQLException;
import java.util.List;

public interface CuponDAO {
    List<Cupon> listarPorUsuario(int idUsuario) throws SQLException;
    Cupon obtener(int id) throws SQLException;
    // Usan la conexión de TransactionContext
    int insertar(Cupon cupon) throws SQLException;
    void desactivar(int id) throws SQLException;
}
