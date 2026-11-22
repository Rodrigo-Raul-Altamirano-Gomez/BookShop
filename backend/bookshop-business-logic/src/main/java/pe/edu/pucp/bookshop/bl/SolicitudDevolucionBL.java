package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.SolicitudDevolucion;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

import java.util.List;

public interface SolicitudDevolucionBL {
    SolicitudDevolucion insertar(SolicitudDevolucion solicitud) throws BusinessLogicException;
    List<SolicitudDevolucion> listarPorUsuario(int idUsuario) throws BusinessLogicException;
    void modificar(SolicitudDevolucion solicitud) throws BusinessLogicException;
    void retirar(int id) throws BusinessLogicException;
    ResultadoPagina<SolicitudDevolucion> listarPendientes(int pagina, int tamano) throws BusinessLogicException;
    void aceptar(SolicitudDevolucion solicitud) throws BusinessLogicException;
    void rechazar(int id) throws BusinessLogicException;
}
