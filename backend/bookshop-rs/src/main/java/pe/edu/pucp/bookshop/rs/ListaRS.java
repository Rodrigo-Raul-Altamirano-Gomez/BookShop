package pe.edu.pucp.bookshop.rs;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import pe.edu.pucp.bookshop.bl.LibroBL;
import pe.edu.pucp.bookshop.bl.ListaLibroBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.impl.LibroBLImpl;
import pe.edu.pucp.bookshop.bl.impl.ListaLibroBLImpl;
import pe.edu.pucp.bookshop.model.TipoLista;
import pe.edu.pucp.bookshop.model.dto.ResultadoLibros;

import java.util.List;

// Deseados y Favoritos (lista: DESEADOS o FAVORITOS)
@Path("listas")
@Produces("application/json")
@Consumes("application/json")
public class ListaRS {

    private LibroBL libroBL = new LibroBLImpl();
    private ListaLibroBL listaBL = new ListaLibroBLImpl();

    @GET
    @Path("{lista}/usuario/{idUsuario}")
    public ResultadoLibros listarLibros(@PathParam("lista") TipoLista lista, @PathParam("idUsuario") int idUsuario,
                                        @BeanParam ParametrosLibros parametros) throws BusinessLogicException {
        return libroBL.listarLista(idUsuario, lista, parametros.aFiltro());
    }

    // En qué listas del cliente está el libro
    @GET
    @Path("usuario/{idUsuario}/libro/{idLibro}")
    public List<TipoLista> listarListas(@PathParam("idUsuario") int idUsuario, @PathParam("idLibro") int idLibro) throws BusinessLogicException {
        return listaBL.listarListas(idUsuario, idLibro);
    }

    @PUT
    @Path("{lista}/usuario/{idUsuario}/libro/{idLibro}")
    public void agregar(@PathParam("lista") TipoLista lista, @PathParam("idUsuario") int idUsuario,
                        @PathParam("idLibro") int idLibro) throws BusinessLogicException {
        listaBL.agregar(idUsuario, idLibro, lista);
    }

    @DELETE
    @Path("{lista}/usuario/{idUsuario}/libro/{idLibro}")
    public void quitar(@PathParam("lista") TipoLista lista, @PathParam("idUsuario") int idUsuario,
                       @PathParam("idLibro") int idLibro) throws BusinessLogicException {
        listaBL.quitar(idUsuario, idLibro, lista);
    }
}
