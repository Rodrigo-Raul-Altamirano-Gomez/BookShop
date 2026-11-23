package pe.edu.pucp.bookshop.dao.configuracion;

import pe.edu.pucp.bookshop.model.configuracion.Configuracion;

import java.sql.SQLException;

public interface ConfiguracionDAO {
    Configuracion obtener() throws SQLException;
    void modificar(Configuracion configuracion) throws SQLException;
}
