package pe.edu.pucp.bookshop.bl.biblioteca;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.biblioteca.TipoLista;

import java.util.List;

// Agregar y quitar libros de Deseados y Favoritos (ERS RF06). La vista de cada lista está en LibroBL.
public interface ListaLibroBL {
    List<TipoLista> listarListas(int idUsuario, int idLibro) throws BusinessLogicException;
    void agregar(int idUsuario, int idLibro, TipoLista lista) throws BusinessLogicException;
    void quitar(int idUsuario, int idLibro, TipoLista lista) throws BusinessLogicException;
}
