package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.Configuracion;

public interface ConfiguracionBL {
    Configuracion obtener() throws BusinessLogicException;
    void modificar(Configuracion configuracion) throws BusinessLogicException;
}
