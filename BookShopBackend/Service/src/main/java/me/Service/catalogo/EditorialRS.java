package me.Service.catalogo;

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
import me.Business.catalogo.EditorialBO;
import me.Business.catalogo.EditorialBOImpl;
import me.Service.Sesion;

@Path("editoriales")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EditorialRS {

    private final EditorialBO editorialBO = new EditorialBOImpl();

    @Context
    private HttpServletRequest peticion;

    @POST
    @Path("busqueda")
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(FiltroReferenciaDTO filtro) throws NegocioException {
        return editorialBO.buscar(filtro);
    }

    @GET
    @Path("{id}")
    public DetalleReferenciaDTO obtenerDetalle(@PathParam("id") int id) throws NegocioException {
        return editorialBO.obtenerDetalle(Sesion.obtenerIdUsuario(peticion), id);
    }
}
