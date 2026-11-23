package pe.edu.pucp.bookshop.bl.resena;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;
import pe.edu.pucp.bookshop.model.resena.ReporteResena;

import java.util.List;

public interface ReporteResenaBL {
    ReporteResena insertar(ReporteResena reporte) throws BusinessLogicException;
    List<ReporteResena> listarPorUsuario(int idUsuario) throws BusinessLogicException;
    void modificar(ReporteResena reporte) throws BusinessLogicException;
    void retirar(int id) throws BusinessLogicException;
    ResultadoPagina<ReporteResena> listarPendientes(int pagina, int tamano) throws BusinessLogicException;
    void aceptar(ReporteResena reporte) throws BusinessLogicException;
    void rechazar(int id) throws BusinessLogicException;
}
