package pe.edu.pucp.bookshop.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import pe.edu.pucp.bookshop.bl.ResenaBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.impl.ResenaBLImpl;
import pe.edu.pucp.bookshop.model.Resena;
import pe.edu.pucp.bookshop.model.dto.ResenasLibro;

import java.util.List;

@Path("resenas")
@Produces("application/json")
@Consumes("application/json")
public class ResenaRS {

    private ResenaBL resenaBL = new ResenaBLImpl();

    // usuario: quien consulta (0: visitante); ocultas: vista del administrador
    @GET
    @Path("libro/{idLibro}")
    public ResenasLibro listarPorLibro(@PathParam("idLibro") int idLibro,
                                       @QueryParam("usuario") @DefaultValue("0") int idUsuario,
                                       @QueryParam("ocultas") @DefaultValue("false") boolean incluirOcultas) throws BusinessLogicException {
        return resenaBL.listarPorLibro(idLibro, idUsuario, incluirOcultas);
    }

    @GET
    @Path("usuario/{idUsuario}")
    public List<Resena> listarPorUsuario(@PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        return resenaBL.listarPorUsuario(idUsuario);
    }

    @POST
    public Resena insertar(Resena resena) throws BusinessLogicException {
        return resenaBL.insertar(resena);
    }

    @PUT
    @Path("{id}")
    public void modificar(@PathParam("id") int id, Resena resena) throws BusinessLogicException {
        resena.setId(id);
        resenaBL.modificar(resena);
    }

    @DELETE
    @Path("{id}")
    public void eliminar(@PathParam("id") int id) throws BusinessLogicException {
        resenaBL.eliminar(id);
    }

    // Marca o desmarca la reseña como útil
    @PUT
    @Path("{id}/util/usuario/{idUsuario}")
    public void alternarVoto(@PathParam("id") int id, @PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        resenaBL.alternarVoto(id, idUsuario);
    }

    @PUT
    @Path("{id}/ocultar")
    public void ocultar(@PathParam("id") int id) throws BusinessLogicException {
        resenaBL.cambiarVisibilidad(id, false);
    }

    @PUT
    @Path("{id}/mostrar")
    public void mostrar(@PathParam("id") int id) throws BusinessLogicException {
        resenaBL.cambiarVisibilidad(id, true);
    }
}
