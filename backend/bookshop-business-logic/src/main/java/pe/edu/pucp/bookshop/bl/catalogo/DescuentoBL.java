package pe.edu.pucp.bookshop.bl.catalogo;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.catalogo.Descuento;

import java.util.List;

public interface DescuentoBL {
    List<Descuento> listarPorLibro(int idLibro) throws BusinessLogicException;
    Descuento insertar(Descuento descuento) throws BusinessLogicException;
    void eliminar(int id) throws BusinessLogicException;
}
