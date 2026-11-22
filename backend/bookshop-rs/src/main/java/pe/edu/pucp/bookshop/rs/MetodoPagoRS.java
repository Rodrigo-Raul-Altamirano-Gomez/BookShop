package pe.edu.pucp.bookshop.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import pe.edu.pucp.bookshop.bl.MetodoPagoBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.impl.MetodoPagoBLImpl;
import pe.edu.pucp.bookshop.model.MetodoPago;

import java.util.List;

@Path("metodos-pago")
@Produces("application/json")
@Consumes("application/json")
public class MetodoPagoRS {

    private MetodoPagoBL metodoPagoBL = new MetodoPagoBLImpl();

    @GET
    @Path("usuario/{idUsuario}")
    public List<MetodoPago> listarPorUsuario(@PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        return metodoPagoBL.listarPorUsuario(idUsuario);
    }

    // Los que se ofrecen al pagar: activos y no vencidos
    @GET
    @Path("usuario/{idUsuario}/vigentes")
    public List<MetodoPago> listarVigentesPorUsuario(@PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        return metodoPagoBL.listarVigentesPorUsuario(idUsuario);
    }

    @POST
    public MetodoPago insertar(MetodoPago metodoPago) throws BusinessLogicException {
        return metodoPagoBL.insertar(metodoPago);
    }

    @PUT
    @Path("{id}")
    public void modificar(@PathParam("id") int id, MetodoPago metodoPago) throws BusinessLogicException {
        metodoPago.setId(id);
        metodoPagoBL.modificar(metodoPago);
    }

    @PUT
    @Path("{id}/predeterminado")
    public void marcarPredeterminado(@PathParam("id") int id, MetodoPago metodoPago) throws BusinessLogicException {
        metodoPago.setId(id);
        metodoPagoBL.marcarPredeterminado(metodoPago);
    }

    @PUT
    @Path("{id}/desactivar")
    public void desactivar(@PathParam("id") int id) throws BusinessLogicException {
        metodoPagoBL.desactivar(id);
    }
}
