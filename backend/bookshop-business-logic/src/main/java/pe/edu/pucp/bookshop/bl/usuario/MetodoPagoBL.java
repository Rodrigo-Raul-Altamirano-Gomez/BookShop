package pe.edu.pucp.bookshop.bl.usuario;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.usuario.MetodoPago;

import java.util.List;

public interface MetodoPagoBL {
    List<MetodoPago> listarPorUsuario(int idUsuario) throws BusinessLogicException;
    List<MetodoPago> listarVigentesPorUsuario(int idUsuario) throws BusinessLogicException;
    MetodoPago insertar(MetodoPago metodoPago) throws BusinessLogicException;
    void modificar(MetodoPago metodoPago) throws BusinessLogicException;
    void marcarPredeterminado(MetodoPago metodoPago) throws BusinessLogicException;
    void desactivar(int id) throws BusinessLogicException;
}
