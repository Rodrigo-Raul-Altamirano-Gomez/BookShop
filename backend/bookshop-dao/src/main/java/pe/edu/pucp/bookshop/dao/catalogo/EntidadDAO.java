package pe.edu.pucp.bookshop.dao.catalogo;

import pe.edu.pucp.bookshop.model.catalogo.Entidad;
import pe.edu.pucp.bookshop.model.catalogo.EntidadAdmin;
import pe.edu.pucp.bookshop.model.catalogo.TipoEntidad;

import java.sql.SQLException;
import java.util.List;

public interface EntidadDAO {
    // activo = null: todos
    int contar(TipoEntidad tipo, String texto, Boolean activo) throws SQLException;
    List<Entidad> listarPagina(TipoEntidad tipo, String texto, Boolean activo, boolean descendente, int pagina, int tamano) throws SQLException;
    List<EntidadAdmin> listarPaginaAdmin(TipoEntidad tipo, String texto, Boolean activo, int pagina, int tamano) throws SQLException;
    List<Entidad> listarActivas(TipoEntidad tipo) throws SQLException;
    Entidad obtener(int id) throws SQLException;
    int insertar(Entidad entidad) throws SQLException;
    void modificar(Entidad entidad) throws SQLException;
    void cambiarActivo(int id, boolean activo) throws SQLException;
}
