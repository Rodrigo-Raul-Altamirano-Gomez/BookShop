package me.Service.recomendacion;

import java.util.List;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Service.Servicios;

@Path("recomendaciones")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class RecomendacionRS {
    @Inject
    private Servicios servicios;

    @GET
    @Path("similares/{idLibro}")
    public List<TarjetaLibroDTO> similares(@PathParam("idLibro") int idLibro) throws NegocioException {
        return servicios.getRecomendacionBO().similares(idLibro);
    }

    @POST
    @Path("complementarios")
    public List<TarjetaLibroDTO> complementarios(List<Integer> idsCarrito) throws NegocioException {
        return servicios.getRecomendacionBO().complementarios(idsCarrito);
    }
}
