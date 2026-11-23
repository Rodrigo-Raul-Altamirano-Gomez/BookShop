package pe.edu.pucp.bookshop.rs.pedido;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.pedido.CuponBL;
import pe.edu.pucp.bookshop.bl.pedido.impl.CuponBLImpl;
import pe.edu.pucp.bookshop.model.pedido.Cupon;

import java.util.List;

@Path("cupones")
@Produces("application/json")
@Consumes("application/json")
public class CuponRS {

    private CuponBL cuponBL = new CuponBLImpl();

    @GET
    @Path("usuario/{idUsuario}")
    public List<Cupon> listarPorUsuario(@PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        return cuponBL.listarPorUsuario(idUsuario);
    }

    @GET
    @Path("usuario/{idUsuario}/utilizables")
    public List<Cupon> listarUtilizables(@PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        return cuponBL.listarUtilizables(idUsuario);
    }
}
