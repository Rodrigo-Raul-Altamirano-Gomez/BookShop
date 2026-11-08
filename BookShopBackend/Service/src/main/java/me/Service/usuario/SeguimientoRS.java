package me.Service.usuario;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.usuario.ConsultaSeguimientosDTO;
import me.Model.dto.usuario.TipoSeguimiento;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("cuenta/seguimientos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class SeguimientoRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest peticion;

    @PUT
    @Path("{tipo}/{idReferencia}")
    public void guardar(
            @PathParam("tipo") TipoSeguimiento tipo,
            @PathParam("idReferencia") int idReferencia) throws NegocioException {
        servicios.getSeguimientoBO().guardar(Sesion.exigirIdUsuario(peticion), tipo, idReferencia);
    }

    @DELETE
    @Path("{tipo}/{idReferencia}")
    public boolean retirar(
            @PathParam("tipo") TipoSeguimiento tipo,
            @PathParam("idReferencia") int idReferencia) throws NegocioException {
        return servicios.getSeguimientoBO().retirar(Sesion.exigirIdUsuario(peticion), tipo, idReferencia);
    }

    @POST
    @Path("busqueda")
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> listar(
            ConsultaSeguimientosDTO consulta) throws NegocioException {
        return servicios.getSeguimientoBO().listar(Sesion.exigirIdUsuario(peticion), consulta);
    }
}
