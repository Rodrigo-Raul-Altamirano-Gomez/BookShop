package me.Service.compra;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.compra.ResumenCompraDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("carrito")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CarritoRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest request;

    @GET
    public ResumenCompraDTO consultar() throws NegocioException {
        return servicios.carrito().consultar(Sesion.exigir(request));
    }

    @POST
    @Path("libros/{idLibro}")
    public ResumenCompraDTO agregar(
            @PathParam("idLibro") int idLibro,
            @QueryParam("cantidad") Integer cantidad) throws NegocioException {
        int actor = Sesion.exigir(request);
        if (cantidad == null) throw new ValidacionException("cantidad", "Indica la cantidad.");
        return servicios.carrito().agregar(actor, idLibro, cantidad);
    }

    @PUT
    @Path("libros/{idLibro}")
    public ResumenCompraDTO cambiarCantidad(
            @PathParam("idLibro") int idLibro,
            @QueryParam("cantidad") Integer cantidad) throws NegocioException {
        int actor = Sesion.exigir(request);
        if (cantidad == null) throw new ValidacionException("cantidad", "Indica la cantidad.");
        return servicios.carrito().cambiarCantidad(actor, idLibro, cantidad);
    }

    @DELETE
    @Path("libros/{idLibro}")
    public ResumenCompraDTO retirar(@PathParam("idLibro") int idLibro) throws NegocioException {
        return servicios.carrito().retirar(Sesion.exigir(request), idLibro);
    }

    @DELETE
    public ResumenCompraDTO vaciar() throws NegocioException {
        return servicios.carrito().vaciar(Sesion.exigir(request));
    }
}
