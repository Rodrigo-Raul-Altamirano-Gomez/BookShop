package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.SeguimientoEntidad;
import pe.edu.pucp.bookshop.model.TipoEntidad;

import java.sql.SQLException;
import java.util.List;

public interface SeguimientoDAO {
    // Lo que sigue el cliente, solo lo activo; texto = null: sin búsqueda
    int contar(int idUsuario, TipoEntidad tipo, String texto) throws SQLException;
    List<Entidad> listarPagina(int idUsuario, TipoEntidad tipo, String texto, String orden, int pagina, int tamano) throws SQLException;
    boolean existe(int idUsuario, int idEntidad) throws SQLException;
    void insertar(SeguimientoEntidad seguimiento) throws SQLException;
    void eliminar(int idUsuario, int idEntidad) throws SQLException;
}
