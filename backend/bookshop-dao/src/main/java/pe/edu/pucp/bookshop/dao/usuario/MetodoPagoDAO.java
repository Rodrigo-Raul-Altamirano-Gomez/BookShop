package pe.edu.pucp.bookshop.dao.usuario;

import pe.edu.pucp.bookshop.model.usuario.MetodoPago;

import java.sql.SQLException;
import java.util.List;

public interface MetodoPagoDAO {
    List<MetodoPago> listarActivosPorUsuario(int idUsuario) throws SQLException;
    int contarActivosConNumero(int idUsuario, String numero, int idExcluido) throws SQLException;
    int insertar(MetodoPago metodoPago) throws SQLException;
    void modificar(MetodoPago metodoPago) throws SQLException;
    void desactivar(int id) throws SQLException;
    // Usan la conexión de TransactionContext
    void quitarPredeterminado(int idUsuario) throws SQLException;
    void marcarPredeterminado(int id) throws SQLException;
}
