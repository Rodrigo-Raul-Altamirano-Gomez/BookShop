package me.Service.opinion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.opinion.ConsultaResenasDTO;
import me.Model.dto.opinion.DatosResenaDTO;
import me.Model.dto.opinion.ResenaDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;
import me.Business.opinion.ResenaBO;
import me.Business.opinion.ResenaBOImpl;
import me.Business.opinion.VotoUtilBO;
import me.Business.opinion.VotoUtilBOImpl;
import me.Service.Sesion;

@Path("resenas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ResenaRS {

    private final ResenaBO resenaBO = new ResenaBOImpl();
    private final VotoUtilBO votoUtilBO = new VotoUtilBOImpl();

    @Context
    private HttpServletRequest peticion;

    @GET
    @Path("libro/{idLibro}/publicacion/permitida")
    public void verificarPuedePublicar(@PathParam("idLibro") int idLibro) throws NegocioException {
        resenaBO.verificarPuedePublicar(Sesion.exigirIdUsuario(peticion), idLibro);
    }

    @POST
    @Path("libro/{idLibro}")
    public int publicar(@PathParam("idLibro") int idLibro, DatosResenaDTO datos) throws NegocioException {
        return resenaBO.publicar(Sesion.exigirIdUsuario(peticion), idLibro, datos);
    }

    @PUT
    @Path("{id}")
    public void modificar(@PathParam("id") int id, DatosResenaDTO datos) throws NegocioException {
        resenaBO.modificar(Sesion.exigirIdUsuario(peticion), id, datos);
    }

    @DELETE
    @Path("{id}")
    public void eliminarPropia(@PathParam("id") int id) throws NegocioException {
        resenaBO.eliminarPropia(Sesion.exigirIdUsuario(peticion), id);
    }

    @POST
    @Path("propias/busqueda")
    public ResultadoPaginadoDTO<ResenaDTO> listarPropias(ConsultaResenasDTO consulta) throws NegocioException {
        return resenaBO.listarPropias(Sesion.exigirIdUsuario(peticion), consulta);
    }

    @PUT
    @Path("{id}/voto")
    public boolean marcarVoto(@PathParam("id") int id) throws NegocioException {
        return votoUtilBO.cambiarVoto(Sesion.exigirIdUsuario(peticion), id, true);
    }

    @DELETE
    @Path("{id}/voto")
    public boolean retirarVoto(@PathParam("id") int id) throws NegocioException {
        return votoUtilBO.cambiarVoto(Sesion.exigirIdUsuario(peticion), id, false);
    }

    @POST
    @Path("libro/{idLibro}/busqueda")
    public ResultadoPaginadoDTO<ResenaDTO> listarPorLibro(@PathParam("idLibro") int idLibro,
        ConsultaResenasDTO consulta) throws NegocioException {
        return resenaBO.listarPorLibro(Sesion.obtenerIdUsuario(peticion), idLibro, consulta);
    }

    @GET
    @Path("libro/{idLibro}/valoracion")
    public ValoracionLibroDTO obtenerValoracion(@PathParam("idLibro") int idLibro) throws NegocioException {
        return resenaBO.obtenerValoracion(idLibro);
    }
}
