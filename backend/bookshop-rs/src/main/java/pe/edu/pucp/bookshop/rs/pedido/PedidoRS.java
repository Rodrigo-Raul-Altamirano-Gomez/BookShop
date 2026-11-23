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
import jakarta.ws.rs.core.Response;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.pedido.PedidoBL;
import pe.edu.pucp.bookshop.bl.pedido.impl.PedidoBLImpl;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;
import pe.edu.pucp.bookshop.model.pedido.EstadoPedido;
import pe.edu.pucp.bookshop.model.pedido.Pedido;
import pe.edu.pucp.bookshop.model.pedido.PedidoDetalle;
import pe.edu.pucp.bookshop.model.pedido.ResumenCompra;
import pe.edu.pucp.bookshop.model.pedido.SolicitudCompra;

@Path("pedidos")
@Produces("application/json")
@Consumes("application/json")
public class PedidoRS {

    private PedidoBL pedidoBL = new PedidoBLImpl();

    // Importes del carrito y del resumen de compra
    @POST
    @Path("resumen")
    public ResumenCompra calcularResumen(SolicitudCompra solicitud) throws BusinessLogicException {
        return pedidoBL.calcularResumen(solicitud);
    }

    // Pagar: revalida, simula el pago y registra el pedido
    @POST
    public Pedido pagar(SolicitudCompra solicitud) throws BusinessLogicException {
        return pedidoBL.pagar(solicitud);
    }

    @GET
    @Path("usuario/{idUsuario}")
    public ResultadoPagina<Pedido> listarPorUsuario(@PathParam("idUsuario") int idUsuario,
                                                   @QueryParam("estado") EstadoPedido estado,
                                                   @QueryParam("orden") String orden,
                                                   @QueryParam("pagina") @DefaultValue("1") int pagina,
                                                   @QueryParam("tamano") @DefaultValue("20") int tamano) throws BusinessLogicException {
        return pedidoBL.listarPorUsuario(idUsuario, estado, orden, pagina, tamano);
    }

    @GET
    @Path("{id}")
    public PedidoDetalle obtener(@PathParam("id") int id) throws BusinessLogicException {
        return pedidoBL.obtener(id);
    }

    @PUT
    @Path("{id}/cancelar")
    public void cancelar(@PathParam("id") int id) throws BusinessLogicException {
        pedidoBL.cancelar(id);
    }

    // El PDF se abre en el navegador (inline)
    @GET
    @Path("{id}/comprobante")
    @Produces("application/pdf")
    public Response generarComprobante(@PathParam("id") int id) throws BusinessLogicException {
        byte[] pdf = pedidoBL.generarComprobante(id);
        return Response.ok(pdf).header("Content-Disposition", "inline; filename=\"comprobante-" + id + ".pdf\"").build();
    }
}
