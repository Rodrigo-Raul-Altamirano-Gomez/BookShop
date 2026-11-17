package me.Service.usuario;

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
import me.Business.usuario.SeguimientoBO;
import me.Business.usuario.SeguimientoBOImpl;
import me.Service.Sesion;

@Path("cuenta/seguimientos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SeguimientoRS {

    private final SeguimientoBO seguimientoBO = new SeguimientoBOImpl();

    @Context
    private HttpServletRequest peticion;

    @PUT
    @Path("{tipo}/{idReferencia}")
    public void guardar(@PathParam("tipo") TipoSeguimiento tipo, @PathParam("idReferencia") int idReferencia)
        throws NegocioException {
        seguimientoBO.guardar(Sesion.exigirIdUsuario(peticion), tipo, idReferencia);
    }

    @DELETE
    @Path("{tipo}/{idReferencia}")
    public boolean retirar(@PathParam("tipo") TipoSeguimiento tipo, @PathParam("idReferencia") int
        idReferencia) throws NegocioException {
        return seguimientoBO.retirar(Sesion.exigirIdUsuario(peticion), tipo, idReferencia);
    }

    @POST
    @Path("busqueda")
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> listar(ConsultaSeguimientosDTO consulta) throws
        NegocioException {
        return seguimientoBO.listar(Sesion.exigirIdUsuario(peticion), consulta);
    }
}
