package me.Service.catalogo;

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
import me.Model.dto.catalogo.DetalleReferenciaDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("editoriales")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class EditorialRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest request;

    @POST
    @Path("busqueda")
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(FiltroReferenciaDTO filtro) throws NegocioException {
        return servicios.editorial().buscar(Sesion.actor(request), filtro);
    }

    @POST
    @Path("opciones")
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscarOpciones(
            FiltroReferenciaDTO filtro) throws NegocioException {
        return servicios.editorial().buscarOpciones(Sesion.actor(request), filtro);
    }

    @GET
    @Path("{id}")
    public DetalleReferenciaDTO obtenerDetalle(@PathParam("id") int id) throws NegocioException {
        return servicios.editorial().obtenerDetalle(Sesion.actor(request), id);
    }
}
