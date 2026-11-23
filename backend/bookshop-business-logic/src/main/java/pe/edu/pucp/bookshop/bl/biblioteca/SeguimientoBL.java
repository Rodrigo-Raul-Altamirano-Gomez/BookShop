package pe.edu.pucp.bookshop.bl.biblioteca;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.catalogo.Entidad;
import pe.edu.pucp.bookshop.model.catalogo.TipoEntidad;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;

public interface SeguimientoBL {
    ResultadoPagina<Entidad> listar(int idUsuario, TipoEntidad tipo, String texto, String orden, int pagina, int tamano) throws BusinessLogicException;
    boolean sigue(int idUsuario, int idEntidad) throws BusinessLogicException;
    void seguir(int idUsuario, int idEntidad) throws BusinessLogicException;
    void dejarDeSeguir(int idUsuario, int idEntidad) throws BusinessLogicException;
}
