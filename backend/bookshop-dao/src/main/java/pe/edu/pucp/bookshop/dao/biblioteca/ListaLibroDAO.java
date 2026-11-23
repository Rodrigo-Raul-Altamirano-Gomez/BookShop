package pe.edu.pucp.bookshop.dao.biblioteca;

import pe.edu.pucp.bookshop.model.biblioteca.TipoLista;

import java.sql.SQLException;
import java.util.List;

// Libros en Deseados y Favoritos (ERS RF06)
public interface ListaLibroDAO {
    List<Integer> listarLibros(int idUsuario, TipoLista lista) throws SQLException;
    // Las listas del cliente en que está el libro
    List<TipoLista> listarListas(int idUsuario, int idLibro) throws SQLException;
    void insertar(int idUsuario, int idLibro, TipoLista lista) throws SQLException;
    void eliminar(int idUsuario, int idLibro, TipoLista lista) throws SQLException;
}
