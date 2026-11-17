package me.Service.compra;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
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
import me.Model.dto.compra.DatosCancelacionDTO;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.FiltroPedidoDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.SeleccionCompraDTO;
import me.Model.dto.compra.TarjetaPedidoDTO;
import me.Business.compra.PedidoBO;
import me.Business.compra.PedidoBOImpl;
import me.Service.Sesion;

@Path("pedidos")
@Produces(MediaType.APPLICATION_JSON)
public class PedidoRS {

    private final PedidoBO pedidoBO = new PedidoBOImpl();

    @Context
    private HttpServletRequest peticion;

    @POST
    @Path("busqueda")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResultadoPaginadoDTO<TarjetaPedidoDTO> listar(FiltroPedidoDTO filtro) throws NegocioException {
        return pedidoBO.listar(Sesion.exigirIdUsuario(peticion), filtro);
    }

    @GET
    @Path("{id}")
    public DetallePedidoDTO consultar(@PathParam("id") int id) throws NegocioException {
        return pedidoBO.consultar(Sesion.exigirIdUsuario(peticion), id);
    }

    @PUT
    @Path("{id}/seguimiento")
    public DetallePedidoDTO actualizarSeguimiento(@PathParam("id") int id) throws NegocioException {
        return pedidoBO.actualizarSeguimiento(Sesion.exigirIdUsuario(peticion), id);
    }

    @POST
    @Path("{id}/cancelacion")
    @Consumes(MediaType.APPLICATION_JSON)
    public DetallePedidoDTO cancelar(@PathParam("id") int id, DatosCancelacionDTO datos) throws NegocioException {
        return pedidoBO.cancelar(Sesion.exigirIdUsuario(peticion), id,
            datos == null ? null : datos.motivo());
    }

    @POST
    @Path("preparacion")
    public PreparacionCompraDTO preparar() throws NegocioException {
        return pedidoBO.preparar(Sesion.exigirIdUsuario(peticion));
    }

    @GET
    @Path("preparacion")
    public PreparacionCompraDTO consultarPreparacion() throws NegocioException {
        return pedidoBO.consultarPreparacion(Sesion.exigirIdUsuario(peticion));
    }

    @PUT
    @Path("preparacion")
    @Consumes(MediaType.APPLICATION_JSON)
    public PreparacionCompraDTO seleccionar(SeleccionCompraDTO seleccion) throws NegocioException {
        return pedidoBO.seleccionar(Sesion.exigirIdUsuario(peticion), seleccion);
    }

    @POST
    @Path("confirmacion")
    public DetallePedidoDTO pagar() throws NegocioException {
        return pedidoBO.pagar(Sesion.exigirIdUsuario(peticion));
    }
}
