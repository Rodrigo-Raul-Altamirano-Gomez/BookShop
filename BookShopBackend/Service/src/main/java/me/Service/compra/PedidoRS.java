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
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class PedidoRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest peticion;

    @POST
    @Path("preparacion")
    public PreparacionCompraDTO preparar() throws NegocioException {
        return servicios.getPedidoBO().preparar(Sesion.exigirIdUsuario(peticion));
    }

    @GET
    @Path("preparacion")
    public PreparacionCompraDTO consultarPreparacion() throws NegocioException {
        return servicios.getPedidoBO().consultarPreparacion(Sesion.exigirIdUsuario(peticion));
    }

    @PUT
    @Path("preparacion")
    @Consumes(MediaType.APPLICATION_JSON)
    public PreparacionCompraDTO seleccionar(SeleccionCompraDTO seleccion) throws NegocioException {
        return servicios.getPedidoBO().seleccionar(Sesion.exigirIdUsuario(peticion), seleccion);
    }

    @POST
    @Path("confirmacion")
    public DetallePedidoDTO pagar() throws NegocioException {
        return servicios.getPedidoBO().pagar(Sesion.exigirIdUsuario(peticion));
    }
}
