package pe.edu.pucp.bookshop.rs.catalogo;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import pe.edu.pucp.bookshop.bl.catalogo.EntidadBL;
import pe.edu.pucp.bookshop.bl.catalogo.impl.EntidadBLImpl;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.catalogo.Entidad;
import pe.edu.pucp.bookshop.model.catalogo.EntidadAdmin;
import pe.edu.pucp.bookshop.model.catalogo.TipoEntidad;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;

import java.util.List;

// Autores, categorías y editoriales
@Path("entidades")
@Produces("application/json")
@Consumes("application/json")
public class EntidadRS {

    private EntidadBL entidadBL = new EntidadBLImpl();

    @GET
    public ResultadoPagina<Entidad> listar(@QueryParam("tipo") TipoEntidad tipo,
                                           @QueryParam("q") String texto,
                                           @QueryParam("orden") String orden,
                                           @QueryParam("pagina") @DefaultValue("1") int pagina,
                                           @QueryParam("tamano") @DefaultValue("24") int tamano) throws BusinessLogicException {
        return entidadBL.listar(tipo, texto, orden, pagina, tamano);
    }

    @GET
    @Path("{id}")
    public Entidad obtener(@PathParam("id") int id) throws BusinessLogicException {
        return entidadBL.obtener(id);
    }

    @GET
    @Path("admin")
    public ResultadoPagina<EntidadAdmin> listarAdmin(@QueryParam("tipo") TipoEntidad tipo,
                                                     @QueryParam("q") String texto,
                                                     @QueryParam("estado") String estado,
                                                     @QueryParam("pagina") @DefaultValue("1") int pagina,
                                                     @QueryParam("tamano") @DefaultValue("20") int tamano) throws BusinessLogicException {
        return entidadBL.listarAdmin(tipo, texto, estado, pagina, tamano);
    }

    @GET
    @Path("activas")
    public List<Entidad> listarActivas(@QueryParam("tipo") TipoEntidad tipo) throws BusinessLogicException {
        return entidadBL.listarActivas(tipo);
    }

    @POST
    public Entidad insertar(Entidad entidad) throws BusinessLogicException {
        return entidadBL.insertar(entidad);
    }

    @PUT
    @Path("{id}")
    public void modificar(@PathParam("id") int id, Entidad entidad) throws BusinessLogicException {
        entidad.setId(id);
        entidadBL.modificar(entidad);
    }

    @PUT
    @Path("{id}/activar")
    public void activar(@PathParam("id") int id) throws BusinessLogicException {
        entidadBL.cambiarActivo(id, true);
    }

    @PUT
    @Path("{id}/desactivar")
    public void desactivar(@PathParam("id") int id) throws BusinessLogicException {
        entidadBL.cambiarActivo(id, false);
    }
}
