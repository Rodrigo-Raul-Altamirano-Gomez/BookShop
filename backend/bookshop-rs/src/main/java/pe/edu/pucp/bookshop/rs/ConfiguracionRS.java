package pe.edu.pucp.bookshop.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import pe.edu.pucp.bookshop.bl.ConfiguracionBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.impl.ConfiguracionBLImpl;
import pe.edu.pucp.bookshop.model.Configuracion;

@Path("configuracion")
@Produces("application/json")
@Consumes("application/json")
public class ConfiguracionRS {

    private ConfiguracionBL configuracionBL = new ConfiguracionBLImpl();

    @GET
    public Configuracion obtener() throws BusinessLogicException {
        return configuracionBL.obtener();
    }

    @PUT
    public void modificar(Configuracion configuracion) throws BusinessLogicException {
        configuracionBL.modificar(configuracion);
    }
}
