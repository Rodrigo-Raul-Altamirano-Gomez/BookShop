package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.TipoLista;
import pe.edu.pucp.bookshop.model.dto.ConteoLibros;
import pe.edu.pucp.bookshop.model.dto.FiltroLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

import java.util.List;

public interface LibroBL {
    ResultadoLibros listarCatalogo(FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarNovedades(FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarPorEntidad(int idEntidad, FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarLista(int idUsuario, TipoLista lista, FiltroLibros filtro) throws BusinessLogicException;
    // Recomendaciones (RF12–RF14): cada una en el orden de su puntaje
    ResultadoLibros listarMasVendidos(FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarMasPopulares(FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarParaTi(int idUsuario, FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarSimilares(int idLibro, FiltroLibros filtro) throws BusinessLogicException;
    ResultadoLibros listarComplementarios(List<Integer> carrito, FiltroLibros filtro) throws BusinessLogicException;
    Libro obtener(int id) throws BusinessLogicException;
    ResultadoPagina<Libro> listarAdmin(String texto, String estado, int pagina, int tamano) throws BusinessLogicException;
    ConteoLibros contar() throws BusinessLogicException;
    Libro insertar(Libro libro) throws BusinessLogicException;
    void modificar(Libro libro, int stockAnterior) throws BusinessLogicException;
    void cambiarActivo(int id, boolean activo) throws BusinessLogicException;
}
