package me.Service.opinion;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.opinion.ConsultaResenasDTO;
import me.Model.dto.opinion.ResenaDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("resenas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class ResenaRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest peticion;

    @POST
    @Path("libro/{idLibro}/busqueda")
    public ResultadoPaginadoDTO<ResenaDTO> listarPorLibro(
            @PathParam("idLibro") int idLibro,
            ConsultaResenasDTO consulta) throws NegocioException {
        return servicios.getResenaBO().listarPorLibro(Sesion.obtenerIdUsuario(peticion), idLibro, consulta);
    }

    @GET
    @Path("libro/{idLibro}/valoracion")
    public ValoracionLibroDTO obtenerValoracion(@PathParam("idLibro") int idLibro) throws NegocioException {
        return servicios.getResenaBO().obtenerValoracion(Sesion.obtenerIdUsuario(peticion), idLibro);
    }
}
