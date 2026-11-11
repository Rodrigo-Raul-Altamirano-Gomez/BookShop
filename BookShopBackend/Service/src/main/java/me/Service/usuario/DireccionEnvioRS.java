package me.Service.usuario;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DatosDireccionEnvioDTO;
import me.Model.dto.compra.DireccionEnvioDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("cuenta/direcciones")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class DireccionEnvioRS {

    @Inject
    private Servicios servicios;

    @Context
    private HttpServletRequest peticion;

    @POST
    public DireccionEnvioDTO registrar(DatosDireccionEnvioDTO datos) throws NegocioException {
        return servicios.getDireccionEnvioBO().registrar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), datos);
    }

    @PUT
    @Path("{idDireccion}")
    public DireccionEnvioDTO modificar(@PathParam("idDireccion") int idDireccion, DatosDireccionEnvioDTO
        datos) throws NegocioException {
        return servicios.getDireccionEnvioBO().modificar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idDireccion, datos);
    }

    @GET
    @Path("{idDireccion}")
    public DireccionEnvioDTO consultar(@PathParam("idDireccion") int idDireccion) throws NegocioException {
        return servicios.getDireccionEnvioBO().consultar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idDireccion);
    }

    @GET
    public ResultadoPaginadoDTO<DireccionEnvioDTO> listar(@DefaultValue("1") @QueryParam("pagina") int pagina,
        @DefaultValue("10") @QueryParam("tamanioPagina") int tamanioPagina) throws NegocioException {
        return servicios.getDireccionEnvioBO().listar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), pagina, tamanioPagina);
    }

    @DELETE
    @Path("{idDireccion}")
    public void eliminar(@PathParam("idDireccion") int idDireccion) throws NegocioException {
        servicios.getDireccionEnvioBO().eliminar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idDireccion);
    }

    @PUT
    @Path("{idDireccion}/predeterminada")
    public void establecerPredeterminado(@PathParam("idDireccion") int idDireccion) throws NegocioException {
        servicios.getDireccionEnvioBO().establecerPredeterminado(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idDireccion);
    }
}
