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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("cuenta/libros-guardados")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class LibroGuardadoRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest request;

    @PUT
    @Path("{idLibro}")
    public void guardar(
            @PathParam("idLibro") int idLibro,
            @QueryParam("favorito") String favorito) throws NegocioException {
        servicios.libroGuardado().guardar(Sesion.exigir(request), idLibro, leerFavorito(favorito));
    }

    @DELETE
    @Path("{idLibro}")
    public boolean retirar(
            @PathParam("idLibro") int idLibro,
            @QueryParam("favorito") String favorito) throws NegocioException {
        return servicios.libroGuardado().retirar(Sesion.exigir(request), idLibro, leerFavorito(favorito));
    }

    @POST
    @Path("busqueda")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> listar(
            @QueryParam("favorito") String favorito,
            FiltroLibroDTO filtro) throws NegocioException {
        return servicios.libroGuardado().listar(Sesion.exigir(request), leerFavorito(favorito), filtro);
    }

    private boolean leerFavorito(String favorito) throws ValidacionException {
        if ("true".equalsIgnoreCase(favorito)) return true;
        if ("false".equalsIgnoreCase(favorito)) return false;
        throw new ValidacionException("favorito", "Indica true para Favoritos o false para Deseados.");
    }
}
