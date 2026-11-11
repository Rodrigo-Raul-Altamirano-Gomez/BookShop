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
import me.Model.dto.compra.DatosMetodoPagoDTO;
import me.Model.dto.compra.MetodoPagoDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("cuenta/metodos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class MetodoPagoRS {

    @Inject
    private Servicios servicios;

    @Context
    private HttpServletRequest peticion;

    @POST
    public MetodoPagoDTO registrar(DatosMetodoPagoDTO datos) throws NegocioException {
        return servicios.getMetodoPagoBO().registrar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), datos);
    }

    @PUT
    @Path("{idMetodo}")
    public MetodoPagoDTO modificar(@PathParam("idMetodo") int idMetodo, DatosMetodoPagoDTO datos) throws
        NegocioException {
        return servicios.getMetodoPagoBO().modificar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idMetodo, datos);
    }

    @GET
    @Path("{idMetodo}")
    public MetodoPagoDTO consultar(@PathParam("idMetodo") int idMetodo) throws NegocioException {
        return servicios.getMetodoPagoBO().consultar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idMetodo);
    }

    @GET
    public ResultadoPaginadoDTO<MetodoPagoDTO> listar(@DefaultValue("1") @QueryParam("pagina") int pagina,
        @DefaultValue("10") @QueryParam("tamanioPagina") int tamanioPagina) throws NegocioException {
        return servicios.getMetodoPagoBO().listar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), pagina, tamanioPagina);
    }

    @DELETE
    @Path("{idMetodo}")
    public void eliminar(@PathParam("idMetodo") int idMetodo) throws NegocioException {
        servicios.getMetodoPagoBO().eliminar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idMetodo);
    }

    @PUT
    @Path("{idMetodo}/predeterminado")
    public void establecerPredeterminado(@PathParam("idMetodo") int idMetodo) throws NegocioException {
        servicios.getMetodoPagoBO().establecerPredeterminado(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idMetodo);
    }
}
