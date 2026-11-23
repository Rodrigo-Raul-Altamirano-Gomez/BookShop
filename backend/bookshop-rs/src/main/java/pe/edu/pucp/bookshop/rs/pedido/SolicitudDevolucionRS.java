package pe.edu.pucp.bookshop.rs.pedido;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.pedido.SolicitudDevolucionBL;
import pe.edu.pucp.bookshop.bl.pedido.impl.SolicitudDevolucionBLImpl;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;
import pe.edu.pucp.bookshop.model.pedido.SolicitudDevolucion;

import java.util.List;

// Solicitudes de devolución de pedidos
@Path("devoluciones")
@Produces("application/json")
@Consumes("application/json")
public class SolicitudDevolucionRS {

    private SolicitudDevolucionBL solicitudBL = new SolicitudDevolucionBLImpl();

    @POST
    public SolicitudDevolucion insertar(SolicitudDevolucion solicitud) throws BusinessLogicException {
        return solicitudBL.insertar(solicitud);
    }

    @GET
    @Path("usuario/{idUsuario}")
    public List<SolicitudDevolucion> listarPorUsuario(@PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        return solicitudBL.listarPorUsuario(idUsuario);
    }

    @PUT
    @Path("{id}")
    public void modificar(@PathParam("id") int id, SolicitudDevolucion solicitud) throws BusinessLogicException {
        solicitud.setId(id);
        solicitudBL.modificar(solicitud);
    }

    @PUT
    @Path("{id}/retirar")
    public void retirar(@PathParam("id") int id) throws BusinessLogicException {
        solicitudBL.retirar(id);
    }

    @GET
    @Path("pendientes")
    public ResultadoPagina<SolicitudDevolucion> listarPendientes(@QueryParam("pagina") @DefaultValue("1") int pagina,
                                                                @QueryParam("tamano") @DefaultValue("20") int tamano) throws BusinessLogicException {
        return solicitudBL.listarPendientes(pagina, tamano);
    }

    @PUT
    @Path("{id}/aceptar")
    public void aceptar(@PathParam("id") int id, SolicitudDevolucion solicitud) throws BusinessLogicException {
        solicitud.setId(id);
        solicitudBL.aceptar(solicitud);
    }

    @PUT
    @Path("{id}/rechazar")
    public void rechazar(@PathParam("id") int id) throws BusinessLogicException {
        solicitudBL.rechazar(id);
    }
}
