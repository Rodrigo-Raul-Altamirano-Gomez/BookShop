package me.Service.usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Business.usuario.LibroGuardadoBO;
import me.Business.usuario.LibroGuardadoBOImpl;
import me.Service.Sesion;

@Path("cuenta/libros-guardados")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LibroGuardadoRS {

    private final LibroGuardadoBO libroGuardadoBO = new LibroGuardadoBOImpl();

    @Context
    private HttpServletRequest peticion;

    @PUT
    @Path("{idLibro}")
    public void guardar(@PathParam("idLibro") int idLibro, @QueryParam("favorito") boolean favorito) throws
        NegocioException {
        libroGuardadoBO.guardar(Sesion.exigirIdUsuario(peticion), idLibro, favorito);
    }

    @DELETE
    @Path("{idLibro}")
    public boolean retirar(@PathParam("idLibro") int idLibro, @QueryParam("favorito") boolean favorito) throws
        NegocioException {
        return libroGuardadoBO.retirar(Sesion.exigirIdUsuario(peticion), idLibro, favorito);
    }

    @POST
    @Path("busqueda")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> listar(@QueryParam("favorito") boolean favorito,
        FiltroLibroDTO filtro) throws NegocioException {
        return libroGuardadoBO.listar(Sesion.exigirIdUsuario(peticion), favorito, filtro);
    }
}
