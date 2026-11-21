package pe.edu.pucp.bookshop.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import pe.edu.pucp.bookshop.bl.DireccionBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.impl.DireccionBLImpl;
import pe.edu.pucp.bookshop.model.Direccion;

import java.util.List;

@Path("direcciones")
@Produces("application/json")
@Consumes("application/json")
public class DireccionRS {

    private DireccionBL direccionBL = new DireccionBLImpl();

    @GET
    @Path("usuario/{idUsuario}")
    public List<Direccion> listarPorUsuario(@PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        return direccionBL.listarPorUsuario(idUsuario);
    }

    @POST
    public Direccion insertar(Direccion direccion) throws BusinessLogicException {
        return direccionBL.insertar(direccion);
    }

    @PUT
    @Path("{id}")
    public void modificar(@PathParam("id") int id, Direccion direccion) throws BusinessLogicException {
        direccion.setId(id);
        direccionBL.modificar(direccion);
    }

    @PUT
    @Path("{id}/predeterminada")
    public void marcarPredeterminada(@PathParam("id") int id, Direccion direccion) throws BusinessLogicException {
        direccion.setId(id);
        direccionBL.marcarPredeterminada(direccion);
    }

    @PUT
    @Path("{id}/desactivar")
    public void desactivar(@PathParam("id") int id) throws BusinessLogicException {
        direccionBL.desactivar(id);
    }
}
