package me.Service.solicitud;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.solicitud.DatosDevolucionDTO;
import me.Model.dto.solicitud.DatosReporteResenaDTO;
import me.Model.dto.solicitud.DetalleDevolucionDTO;
import me.Model.dto.solicitud.DetalleReporteResenaDTO;
import me.Model.dto.solicitud.FiltroSolicitudDTO;
import me.Model.dto.solicitud.ResumenSolicitudDTO;
import me.Model.entities.solicitud.EstadoSolicitud;
import me.Business.solicitud.SolicitudBO;
import me.Business.solicitud.SolicitudBOImpl;
import me.Service.Sesion;

@Path("solicitudes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SolicitudRS {

    private final SolicitudBO solicitudBO = new SolicitudBOImpl();

    @Context
    private HttpServletRequest peticion;

    @GET
    @Path("devoluciones/pedido/{idPedido}/permitido")
    public void comprobarDevolucion(@PathParam("idPedido") int idPedido) throws NegocioException {
        solicitudBO.comprobarDevolucion(Sesion.exigirIdUsuario(peticion), idPedido);
    }

    @POST
    @Path("devoluciones/pedido/{idPedido}")
    public DetalleDevolucionDTO enviarDevolucion(@PathParam("idPedido") int idPedido,
        DatosDevolucionDTO datos) throws NegocioException {
        return solicitudBO.enviarDevolucion(Sesion.exigirIdUsuario(peticion), idPedido, datos);
    }

    @POST
    @Path("propias/busqueda")
    public ResultadoPaginadoDTO<ResumenSolicitudDTO> listarPropias(FiltroSolicitudDTO filtro) throws NegocioException {
        return solicitudBO.listarPropias(Sesion.exigirIdUsuario(peticion), filtro);
    }

    @GET
    @Path("devoluciones/{id}")
    public DetalleDevolucionDTO consultarDevolucion(@PathParam("id") int id) throws NegocioException {
        return solicitudBO.consultarDevolucion(Sesion.exigirIdUsuario(peticion), id);
    }

    @GET
    @Path("reportes/{id}")
    public DetalleReporteResenaDTO consultarReporte(@PathParam("id") int id) throws NegocioException {
        return solicitudBO.consultarReporte(Sesion.exigirIdUsuario(peticion), id);
    }

    @PUT
    @Path("devoluciones/{id}")
    public DetalleDevolucionDTO editarDevolucion(@PathParam("id") int id, DatosDevolucionDTO datos) throws NegocioException {
        return solicitudBO.editarDevolucion(Sesion.exigirIdUsuario(peticion), id, datos);
    }

    @PUT
    @Path("reportes/{id}")
    public DetalleReporteResenaDTO editarReporte(@PathParam("id") int id, DatosReporteResenaDTO datos) throws NegocioException {
        return solicitudBO.editarReporte(Sesion.exigirIdUsuario(peticion), id, datos);
    }

    @DELETE
    @Path("{id}")
    public void retirar(@PathParam("id") int id) throws NegocioException {
        solicitudBO.retirar(Sesion.exigirIdUsuario(peticion), id);
    }

    @GET
    @Path("{id}/pedido")
    public DetallePedidoDTO consultarPedidoReferencia(@PathParam("id") int id) throws NegocioException {
        return solicitudBO.consultarPedidoReferencia(Sesion.exigirIdUsuario(peticion), id);
    }

    @POST
    @Path("moderacion/busqueda")
    public ResultadoPaginadoDTO<ResumenSolicitudDTO> listarModeracion(FiltroSolicitudDTO filtro) throws NegocioException {
        Sesion.exigirIdUsuario(peticion);
        return solicitudBO.listarModeracion(filtro);
    }

    @PUT
    @Path("devoluciones/{id}/resolucion")
    public DetalleDevolucionDTO resolverDevolucion(@PathParam("id") int id,
        @QueryParam("resultado") EstadoSolicitud resultado) throws NegocioException {
        return solicitudBO.resolverDevolucion(Sesion.exigirIdUsuario(peticion), id, resultado);
    }

    @PUT
    @Path("reportes/{id}/resolucion")
    public DetalleReporteResenaDTO resolverReporte(@PathParam("id") int id,
        @QueryParam("resultado") EstadoSolicitud resultado) throws NegocioException {
        return solicitudBO.resolverReporte(Sesion.exigirIdUsuario(peticion), id, resultado);
    }

    @GET
    @Path("reportes/resena/{idResena}/permitido")
    public void comprobarReporte(@PathParam("idResena") int idResena) throws NegocioException {
        solicitudBO.comprobarReporte(Sesion.exigirIdUsuario(peticion), idResena);
    }

    @POST
    @Path("reportes/resena/{idResena}")
    public DetalleReporteResenaDTO enviarReporte(@PathParam("idResena") int idResena,
        DatosReporteResenaDTO datos) throws NegocioException {
        return solicitudBO.enviarReporte(Sesion.exigirIdUsuario(peticion), idResena, datos);
    }
}
