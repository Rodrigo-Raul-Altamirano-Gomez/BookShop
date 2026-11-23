package pe.edu.pucp.bookshop.bl.configuracion.impl;

import pe.edu.pucp.bookshop.bl.configuracion.ConfiguracionBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.configuracion.ConfiguracionDAO;
import pe.edu.pucp.bookshop.dao.configuracion.impl.ConfiguracionDAOImpl;
import pe.edu.pucp.bookshop.model.configuracion.Configuracion;

import java.sql.SQLException;

public class ConfiguracionBLImpl implements ConfiguracionBL {

    private ConfiguracionDAO configuracionDAO = new ConfiguracionDAOImpl();

    @Override
    public Configuracion obtener() throws BusinessLogicException {
        try {
            return configuracionDAO.obtener();
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RN13: los cambios rigen solo para las operaciones posteriores (cada operación lee la configuración al ejecutarse)
    @Override
    public void modificar(Configuracion configuracion) throws BusinessLogicException {
        try {
            configuracionDAO.modificar(configuracion);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }
}
