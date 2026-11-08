package me.Service.catalogo;

import java.util.List;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DetalleLibroDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("libros")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class LibroRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest peticion;

    @POST
    @Path("busqueda")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscar(FiltroLibroDTO filtro) throws NegocioException {
        return servicios.getLibroBO().buscar(Sesion.obtenerIdUsuario(peticion), filtro);
    }

    @POST
    @Path("por-autor/{idAutor}")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorAutor(
            @PathParam("idAutor") int idAutor,
            FiltroLibroDTO filtro) throws NegocioException {
        return servicios.getLibroBO().buscarPorAutor(Sesion.obtenerIdUsuario(peticion), idAutor, filtro);
    }

    @POST
    @Path("por-categoria/{idCategoria}")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorCategoria(
            @PathParam("idCategoria") int idCategoria,
            FiltroLibroDTO filtro) throws NegocioException {
        return servicios.getLibroBO().buscarPorCategoria(Sesion.obtenerIdUsuario(peticion), idCategoria, filtro);
    }

    @POST
    @Path("por-editorial/{idEditorial}")
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorEditorial(
            @PathParam("idEditorial") int idEditorial,
            FiltroLibroDTO filtro) throws NegocioException {
        return servicios.getLibroBO().buscarPorEditorial(Sesion.obtenerIdUsuario(peticion), idEditorial, filtro);
    }

    @GET
    @Path("idiomas")
    public ResultadoPaginadoDTO<String> buscarIdiomas(
            @QueryParam("nombre") String nombre,
            @DefaultValue("1") @QueryParam("pagina") int pagina,
            @DefaultValue("12") @QueryParam("tamanioPagina") int tamanioPagina) throws NegocioException {
        return servicios.getLibroBO().buscarIdiomas(Sesion.obtenerIdUsuario(peticion), nombre, pagina, tamanioPagina);
    }

    @GET
    @Path("{idLibro}")
    public DetalleLibroDTO obtenerDetalle(@PathParam("idLibro") int idLibro) throws NegocioException {
        return servicios.getLibroBO().obtenerDetalle(Sesion.obtenerIdUsuario(peticion), idLibro);
    }

    @GET
    @Path("comparacion")
    public List<DetalleLibroDTO> comparar(
            @QueryParam("primerLibro") int primerLibro,
            @QueryParam("segundoLibro") int segundoLibro) throws NegocioException {
        return servicios.getLibroBO().comparar(Sesion.obtenerIdUsuario(peticion), primerLibro, segundoLibro);
    }
}
