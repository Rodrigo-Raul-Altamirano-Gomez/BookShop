package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.dto.ConteoLibros;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface LibroDAO {
    // hoy: fecha con la que se busca el descuento vigente de cada libro
    List<Libro> listarActivos(LocalDate hoy) throws SQLException;
    Libro obtener(int id, LocalDate hoy) throws SQLException;
    // activo = null: todos
    int contarAdmin(String texto, Boolean activo) throws SQLException;
    List<Libro> listarPaginaAdmin(String texto, Boolean activo, int pagina, int tamano, LocalDate hoy) throws SQLException;
    ConteoLibros contarTodos() throws SQLException;
    int insertar(Libro libro) throws SQLException;
    void modificar(Libro libro) throws SQLException;
    void cambiarActivo(int id, boolean activo) throws SQLException;
    // Usan la conexión de TransactionContext
    void bloquear(int id) throws SQLException;
    void descontarStock(int id, int cantidad) throws SQLException;
    void reponerStock(int idPedido) throws SQLException;
}
