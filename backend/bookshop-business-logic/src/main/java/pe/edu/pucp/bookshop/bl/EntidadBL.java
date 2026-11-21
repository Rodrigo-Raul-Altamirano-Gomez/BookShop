package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.TipoEntidad;
import pe.edu.pucp.bookshop.model.dto.EntidadAdmin;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

import java.util.List;

public interface EntidadBL {
    ResultadoPagina<Entidad> listar(TipoEntidad tipo, String texto, String orden, int pagina, int tamano) throws BusinessLogicException;
    Entidad obtener(int id) throws BusinessLogicException;
    ResultadoPagina<EntidadAdmin> listarAdmin(TipoEntidad tipo, String texto, String estado, int pagina, int tamano) throws BusinessLogicException;
    List<Entidad> listarActivas(TipoEntidad tipo) throws BusinessLogicException;
    Entidad insertar(Entidad entidad) throws BusinessLogicException;
    void modificar(Entidad entidad) throws BusinessLogicException;
    void cambiarActivo(int id, boolean activo) throws BusinessLogicException;
}
