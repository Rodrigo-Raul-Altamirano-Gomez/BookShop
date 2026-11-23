package pe.edu.pucp.bookshop.dao.catalogo;

import pe.edu.pucp.bookshop.model.catalogo.Descuento;

import java.sql.SQLException;
import java.util.List;

public interface DescuentoDAO {
    List<Descuento> listarPorLibro(int idLibro) throws SQLException;
    int insertar(Descuento descuento) throws SQLException;
    void eliminar(int id) throws SQLException;
}
