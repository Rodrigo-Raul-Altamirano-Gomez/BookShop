package me.Service.usuario;

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
import me.Business.usuario.MetodoPagoBO;
import me.Business.usuario.MetodoPagoBOImpl;
import me.Service.Sesion;

@Path("cuenta/metodos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MetodoPagoRS {

    private final MetodoPagoBO metodoPagoBO = new MetodoPagoBOImpl();

    @Context
    private HttpServletRequest peticion;

    @POST
    public MetodoPagoDTO registrar(DatosMetodoPagoDTO datos) throws NegocioException {
        return metodoPagoBO.registrar(Sesion.exigirIdUsuario(peticion), datos);
    }

    @PUT
    @Path("{idMetodo}")
    public MetodoPagoDTO modificar(@PathParam("idMetodo") int idMetodo, DatosMetodoPagoDTO datos) throws
        NegocioException {
        return metodoPagoBO.modificar(Sesion.exigirIdUsuario(peticion), idMetodo, datos);
    }

    @GET
    @Path("{idMetodo}")
    public MetodoPagoDTO consultar(@PathParam("idMetodo") int idMetodo) throws NegocioException {
        return metodoPagoBO.consultar(Sesion.exigirIdUsuario(peticion), idMetodo);
    }

    @GET
    public ResultadoPaginadoDTO<MetodoPagoDTO> listar(@DefaultValue("1") @QueryParam("pagina") int pagina,
        @DefaultValue("10") @QueryParam("tamanioPagina") int tamanioPagina) throws NegocioException {
        return metodoPagoBO.listar(Sesion.exigirIdUsuario(peticion), pagina, tamanioPagina);
    }

    @DELETE
    @Path("{idMetodo}")
    public void eliminar(@PathParam("idMetodo") int idMetodo) throws NegocioException {
        metodoPagoBO.eliminar(Sesion.exigirIdUsuario(peticion), idMetodo);
    }

    @PUT
    @Path("{idMetodo}/predeterminado")
    public void establecerPredeterminado(@PathParam("idMetodo") int idMetodo) throws NegocioException {
        metodoPagoBO.establecerPredeterminado(Sesion.exigirIdUsuario(peticion), idMetodo);
    }
}
