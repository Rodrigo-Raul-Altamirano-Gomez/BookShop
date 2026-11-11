package me.Service.usuario;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.BadRequestException;
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
    private HttpServletRequest peticion;

    @PUT
    @Path("{idLibro}")
    public void guardar(@PathParam("idLibro") int idLibro, @QueryParam("favorito") String favorito) throws
        NegocioException {
        if (!"true".equalsIgnoreCase(favorito) && !"false".equalsIgnoreCase(favorito))
            throw new BadRequestException("Selecciona Favoritos o Deseados.");
        boolean esFavorito = Boolean.parseBoolean(favorito);
        servicios.getLibroGuardadoBO().guardar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idLibro, esFavorito);
    }

    @DELETE
    @Path("{idLibro}")
    public boolean retirar(@PathParam("idLibro") int idLibro, @QueryParam("favorito") String favorito) throws
        NegocioException {
        if (!"true".equalsIgnoreCase(favorito) && !"false".equalsIgnoreCase(favorito))
            throw new BadRequestException("Selecciona Favoritos o Deseados.");
        boolean esFavorito = Boolean.parseBoolean(favorito);
        return servicios.getLibroGuardadoBO().retirar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), idLibro, esFavorito);
    }

    @POST
    @Path("busqueda")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> listar(@QueryParam("favorito") String favorito,
        FiltroLibroDTO filtro) throws NegocioException {
        if (!"true".equalsIgnoreCase(favorito) && !"false".equalsIgnoreCase(favorito))
            throw new BadRequestException("Selecciona Favoritos o Deseados.");
        boolean esFavorito = Boolean.parseBoolean(favorito);
        return servicios.getLibroGuardadoBO().listar(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), esFavorito, filtro);
    }
}
