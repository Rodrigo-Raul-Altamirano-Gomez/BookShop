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
    private HttpServletRequest peticion;

    @GET
    public ResumenCompraDTO consultar() throws NegocioException {
        return servicios.getCarritoBO().consultar(Sesion.exigirIdUsuario(peticion));
    }

    @POST
    @Path("libros/{idLibro}")
    public ResumenCompraDTO agregar(
            @PathParam("idLibro") int idLibro,
            @QueryParam("cantidad") Integer cantidad) throws NegocioException {
        int idUsuario = Sesion.exigirIdUsuario(peticion);
        if (cantidad == null) throw new ValidacionException("cantidad", "Indica la cantidad.");
        return servicios.getCarritoBO().agregar(idUsuario, idLibro, cantidad);
    }

    @PUT
    @Path("libros/{idLibro}")
    public ResumenCompraDTO cambiarCantidad(
            @PathParam("idLibro") int idLibro,
            @QueryParam("cantidad") Integer cantidad) throws NegocioException {
        int idUsuario = Sesion.exigirIdUsuario(peticion);
        if (cantidad == null) throw new ValidacionException("cantidad", "Indica la cantidad.");
        return servicios.getCarritoBO().cambiarCantidad(idUsuario, idLibro, cantidad);
    }

    @DELETE
    @Path("libros/{idLibro}")
    public ResumenCompraDTO retirar(@PathParam("idLibro") int idLibro) throws NegocioException {
        return servicios.getCarritoBO().retirar(Sesion.exigirIdUsuario(peticion), idLibro);
    }

    @DELETE
    public ResumenCompraDTO vaciar() throws NegocioException {
        return servicios.getCarritoBO().vaciar(Sesion.exigirIdUsuario(peticion));
    }
}
