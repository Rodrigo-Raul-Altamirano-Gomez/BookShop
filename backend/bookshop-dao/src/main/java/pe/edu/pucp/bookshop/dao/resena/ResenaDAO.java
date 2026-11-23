package pe.edu.pucp.bookshop.dao.resena;

import pe.edu.pucp.bookshop.model.resena.Resena;

import java.sql.SQLException;
import java.util.List;

public interface ResenaDAO {
    // Todas, también las ocultas, de la más reciente a la más antigua; votadaPorMi según idUsuario (0: visitante)
    List<Resena> listarPorLibro(int idLibro, int idUsuario) throws SQLException;
    List<Resena> listarPorUsuario(int idUsuario) throws SQLException;
    // Las visibles de todos los libros (libro, puntuación y fecha), para las recomendaciones
    List<Resena> listarActivas() throws SQLException;
    Resena obtener(int id) throws SQLException;
    int insertar(Resena resena) throws SQLException;
    void modificar(Resena resena) throws SQLException;
    boolean existeVoto(int idResena, int idUsuario) throws SQLException;
    void insertarVoto(int idResena, int idUsuario) throws SQLException;
    void eliminarVoto(int idResena, int idUsuario) throws SQLException;
    // Usan la conexión de TransactionContext
    void cambiarActiva(int id, boolean activa) throws SQLException;
    void eliminarVotos(int idResena) throws SQLException;
    void eliminar(int id) throws SQLException;
}
