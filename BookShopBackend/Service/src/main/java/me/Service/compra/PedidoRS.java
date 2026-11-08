package me.Service.compra;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.SeleccionCompraDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("pedidos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class PedidoRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest request;

    @POST
    @Path("preparacion")
    public PreparacionCompraDTO preparar() throws NegocioException {
        return servicios.pedido().preparar(Sesion.exigir(request));
    }

    @GET
    @Path("preparacion")
    public PreparacionCompraDTO consultarPreparacion() throws NegocioException {
        return servicios.pedido().consultarPreparacion(Sesion.exigir(request));
    }

    @PUT
    @Path("preparacion")
    public PreparacionCompraDTO seleccionar(SeleccionCompraDTO seleccion) throws NegocioException {
        return servicios.pedido().seleccionar(Sesion.exigir(request), seleccion);
    }

    @POST
    @Path("confirmacion")
    public DetallePedidoDTO pagar() throws NegocioException {
        return servicios.pedido().pagar(Sesion.exigir(request));
    }
}
