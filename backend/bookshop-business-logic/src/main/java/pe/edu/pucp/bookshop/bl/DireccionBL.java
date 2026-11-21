package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.Direccion;

import java.util.List;

public interface DireccionBL {
    List<Direccion> listarPorUsuario(int idUsuario) throws BusinessLogicException;
    Direccion insertar(Direccion direccion) throws BusinessLogicException;
    void modificar(Direccion direccion) throws BusinessLogicException;
    void marcarPredeterminada(Direccion direccion) throws BusinessLogicException;
    void desactivar(int id) throws BusinessLogicException;
}
