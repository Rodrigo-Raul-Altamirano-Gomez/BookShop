package pe.edu.pucp.bookshop.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import pe.edu.pucp.bookshop.bl.SeguimientoBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.impl.SeguimientoBLImpl;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.TipoEntidad;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

// Autores, categorías y editoriales que sigue un cliente
@Path("seguimientos")
@Produces("application/json")
@Consumes("application/json")
public class SeguimientoRS {

    private SeguimientoBL seguimientoBL = new SeguimientoBLImpl();

    @GET
    @Path("usuario/{idUsuario}")
    public ResultadoPagina<Entidad> listar(@PathParam("idUsuario") int idUsuario,
                                           @QueryParam("tipo") TipoEntidad tipo,
                                           @QueryParam("q") String texto,
                                           @QueryParam("orden") String orden,
                                           @QueryParam("pagina") @DefaultValue("1") int pagina,
                                           @QueryParam("tamano") @DefaultValue("24") int tamano) throws BusinessLogicException {
        return seguimientoBL.listar(idUsuario, tipo, texto, orden, pagina, tamano);
    }

    @GET
    @Path("usuario/{idUsuario}/entidad/{idEntidad}")
    public boolean sigue(@PathParam("idUsuario") int idUsuario, @PathParam("idEntidad") int idEntidad) throws BusinessLogicException {
        return seguimientoBL.sigue(idUsuario, idEntidad);
    }

    @PUT
    @Path("usuario/{idUsuario}/entidad/{idEntidad}")
    public void seguir(@PathParam("idUsuario") int idUsuario, @PathParam("idEntidad") int idEntidad) throws BusinessLogicException {
        seguimientoBL.seguir(idUsuario, idEntidad);
    }

    @DELETE
    @Path("usuario/{idUsuario}/entidad/{idEntidad}")
    public void dejarDeSeguir(@PathParam("idUsuario") int idUsuario, @PathParam("idEntidad") int idEntidad) throws BusinessLogicException {
        seguimientoBL.dejarDeSeguir(idUsuario, idEntidad);
    }
}
