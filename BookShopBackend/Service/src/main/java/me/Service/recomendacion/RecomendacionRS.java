package me.Service.recomendacion;

import java.util.List;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import me.Business.exceptions.NegocioException;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.dto.catalogo.ConsultaRecomendacionesDTO;
import me.Model.dto.catalogo.RecomendacionesDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("recomendaciones")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class RecomendacionRS {

    @Inject
    private Servicios servicios;

    @Context
    private HttpServletRequest peticion;

    @GET
    @Path("para-ti/disponible")
    public boolean puedeMostrarParaTi() throws NegocioException {
        return servicios.getRecomendacionBO().puedeMostrarParaTi(Sesion.obtenerIdUsuario(peticion));
    }

    @POST
    @Path("populares")
    public RecomendacionesDTO populares(ConsultaRecomendacionesDTO consulta) throws NegocioException {
        return servicios.getRecomendacionBO().abrirPopulares(consulta);
    }

    @POST
    @Path("para-ti")
    public RecomendacionesDTO paraTi(ConsultaRecomendacionesDTO consulta) throws NegocioException {
        return servicios.getRecomendacionBO().abrirParaTi(Sesion.exigirIdUsuario(peticion), consulta);
    }

    @POST
    @Path("consultas/{id}")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> consultar(@PathParam("id") String id,
        ConsultaRecomendacionesDTO consulta) throws NegocioException {
        return servicios.getRecomendacionBO().consultar(Sesion.obtenerIdUsuario(peticion), id, consulta);
    }

    @DELETE
    @Path("consultas/{id}")
    public void descartar(@PathParam("id") String id) throws NegocioException {
        servicios.getRecomendacionBO().descartar(Sesion.obtenerIdUsuario(peticion), id);
    }

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
