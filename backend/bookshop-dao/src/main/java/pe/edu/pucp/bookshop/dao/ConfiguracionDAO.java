package pe.edu.pucp.bookshop.dao;

import pe.edu.pucp.bookshop.model.Configuracion;

import java.sql.SQLException;

public interface ConfiguracionDAO {
    Configuracion obtener() throws SQLException;
    void modificar(Configuracion configuracion) throws SQLException;
}
