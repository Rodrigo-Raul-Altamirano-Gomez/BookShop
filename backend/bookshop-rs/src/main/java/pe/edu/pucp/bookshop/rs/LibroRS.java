package pe.edu.pucp.bookshop.rs;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import pe.edu.pucp.bookshop.bl.LibroBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.impl.LibroBLImpl;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.dto.ConteoLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

import java.util.List;

@Path("libros")
@Produces("application/json")
@Consumes("application/json")
public class LibroRS {

    private LibroBL libroBL = new LibroBLImpl();

    @GET
    public ResultadoLibros listarCatalogo(@BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarCatalogo(parametros.aFiltro());
    }

    @GET
    @Path("novedades")
    public ResultadoLibros listarNovedades(@BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarNovedades(parametros.aFiltro());
    }

    @GET
    @Path("entidad/{idEntidad}")
    public ResultadoLibros listarPorEntidad(@PathParam("idEntidad") int idEntidad, @BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarPorEntidad(idEntidad, parametros.aFiltro());
    }

    // Recomendaciones (RF12–RF14), con los mismos parámetros que toda vista de resultados
    @GET
    @Path("mas-vendidos")
    public ResultadoLibros listarMasVendidos(@BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarMasVendidos(parametros.aFiltro());
    }

    @GET
    @Path("mas-populares")
    public ResultadoLibros listarMasPopulares(@BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarMasPopulares(parametros.aFiltro());
    }

    @GET
    @Path("para-ti/usuario/{idUsuario}")
    public ResultadoLibros listarParaTi(@PathParam("idUsuario") int idUsuario, @BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarParaTi(idUsuario, parametros.aFiltro());
    }

    @GET
    @Path("{id}/similares")
    public ResultadoLibros listarSimilares(@PathParam("id") int id, @BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarSimilares(id, parametros.aFiltro());
    }

    // carrito: los ids de los libros del carrito (?carrito=1&carrito=5)
    @GET
    @Path("complementarios")
    public ResultadoLibros listarComplementarios(@QueryParam("carrito") List<Integer> carrito, @BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarComplementarios(carrito, parametros.aFiltro());
    }

    @GET
    @Path("{id}")
    public Libro obtener(@PathParam("id") int id) throws BusinessLogicException {
        return libroBL.obtener(id);
    }

    @GET
    @Path("admin")
    public ResultadoPagina<Libro> listarAdmin(@QueryParam("q") String texto,
                                              @QueryParam("estado") String estado,
                                              @QueryParam("pagina") @DefaultValue("1") int pagina,
                                              @QueryParam("tamano") @DefaultValue("20") int tamano) throws BusinessLogicException {
        return libroBL.listarAdmin(texto, estado, pagina, tamano);
    }

    @GET
    @Path("conteo")
    public ConteoLibros contar() throws BusinessLogicException {
        return libroBL.contar();
    }

    @POST
    public Libro insertar(Libro libro) throws BusinessLogicException {
        return libroBL.insertar(libro);
    }

    // stockAnterior: el stock que tenía el libro al abrir la edición
    @PUT
    @Path("{id}")
    public void modificar(@PathParam("id") int id, @QueryParam("stockAnterior") int stockAnterior, Libro libro) throws BusinessLogicException {
        libro.setId(id);
        libroBL.modificar(libro, stockAnterior);
    }

    @PUT
    @Path("{id}/activar")
    public void activar(@PathParam("id") int id) throws BusinessLogicException {
        libroBL.cambiarActivo(id, true);
    }

    @PUT
    @Path("{id}/desactivar")
    public void desactivar(@PathParam("id") int id) throws BusinessLogicException {
        libroBL.cambiarActivo(id, false);
    }
}
