package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.dto.ConteoLibros;
import pe.edu.pucp.bookshop.model.dto.FiltroLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

public interface LibroBL {
    ResultadoLibros listarCatalogo(FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarNovedades(FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarPorEntidad(int idEntidad, FiltroLibros filtro) throws BusinessLogicException;
    Libro obtener(int id) throws BusinessLogicException;
    ResultadoPagina<Libro> listarAdmin(String texto, String estado, int pagina, int tamano) throws BusinessLogicException;
    ConteoLibros contar() throws BusinessLogicException;
    Libro insertar(Libro libro) throws BusinessLogicException;
    void modificar(Libro libro) throws BusinessLogicException;
    void cambiarActivo(int id, boolean activo) throws BusinessLogicException;
}
