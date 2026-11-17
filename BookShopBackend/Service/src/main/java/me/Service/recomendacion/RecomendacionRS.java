package me.Service.recomendacion;

import java.util.List;
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
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Business.recomendacion.RecomendacionBO;
import me.Business.recomendacion.RecomendacionBOImpl;
import me.Service.Sesion;

@Path("recomendaciones")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RecomendacionRS {

    private final RecomendacionBO recomendacionBO = new RecomendacionBOImpl();

    @Context
    private HttpServletRequest peticion;

    @GET
    @Path("para-ti/disponible")
    public boolean puedeMostrarParaTi() throws NegocioException {
        return recomendacionBO.puedeMostrarParaTi(Sesion.obtenerIdUsuario(peticion));
    }

    @POST
    @Path("populares")
    public RecomendacionesDTO populares(ConsultaRecomendacionesDTO consulta) throws NegocioException {
        return recomendacionBO.abrirPopulares(consulta);
    }

    @POST
    @Path("para-ti")
    public RecomendacionesDTO paraTi(ConsultaRecomendacionesDTO consulta) throws NegocioException {
        return recomendacionBO.abrirParaTi(Sesion.exigirIdUsuario(peticion), consulta);
    }

    @POST
    @Path("consultas/{id}/busqueda")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> filtrar(@PathParam("id") String id,
        FiltroLibroDTO filtro) throws NegocioException {
        return recomendacionBO.filtrar(Sesion.obtenerIdUsuario(peticion), id, filtro);
    }

    @DELETE
    @Path("consultas/{id}")
    public void descartar(@PathParam("id") String id) throws NegocioException {
        recomendacionBO.descartar(Sesion.obtenerIdUsuario(peticion), id);
    }

    @GET
    @Path("similares/{idLibro}")
    public List<TarjetaLibroDTO> similares(@PathParam("idLibro") int idLibro) throws NegocioException {
        return recomendacionBO.similares(idLibro);
    }

    @POST
    @Path("complementarios")
    public List<TarjetaLibroDTO> complementarios(List<Integer> idsCarrito) throws NegocioException {
        return recomendacionBO.complementarios(idsCarrito);
    }
}
