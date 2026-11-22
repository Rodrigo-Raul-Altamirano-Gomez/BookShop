package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.Cupon;

import java.util.List;

public interface CuponBL {
    List<Cupon> listarPorUsuario(int idUsuario) throws BusinessLogicException;
    List<Cupon> listarUtilizables(int idUsuario) throws BusinessLogicException;
    // Lo llama el hilo del sistema cada minuto (RF03.4)
    void emitirCupones() throws BusinessLogicException;
}
