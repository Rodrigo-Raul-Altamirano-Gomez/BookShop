package pe.edu.pucp.bookshop.rs.resena;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.resena.ReporteResenaBL;
import pe.edu.pucp.bookshop.bl.resena.impl.ReporteResenaBLImpl;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;
import pe.edu.pucp.bookshop.model.resena.ReporteResena;

import java.util.List;

@Path("reportes-resenas")
@Produces("application/json")
@Consumes("application/json")
public class ReporteResenaRS {

    private ReporteResenaBL reporteBL = new ReporteResenaBLImpl();

    @POST
    public ReporteResena insertar(ReporteResena reporte) throws BusinessLogicException {
        return reporteBL.insertar(reporte);
    }

    @GET
    @Path("usuario/{idUsuario}")
    public List<ReporteResena> listarPorUsuario(@PathParam("idUsuario") int idUsuario) throws BusinessLogicException {
        return reporteBL.listarPorUsuario(idUsuario);
    }

    @PUT
    @Path("{id}")
    public void modificar(@PathParam("id") int id, ReporteResena reporte) throws BusinessLogicException {
        reporte.setId(id);
        reporteBL.modificar(reporte);
    }

    @PUT
    @Path("{id}/retirar")
    public void retirar(@PathParam("id") int id) throws BusinessLogicException {
        reporteBL.retirar(id);
    }

    @GET
    @Path("pendientes")
    public ResultadoPagina<ReporteResena> listarPendientes(@QueryParam("pagina") @DefaultValue("1") int pagina,
                                                         @QueryParam("tamano") @DefaultValue("10") int tamano) throws BusinessLogicException {
        return reporteBL.listarPendientes(pagina, tamano);
    }

    @PUT
    @Path("{id}/aceptar")
    public void aceptar(@PathParam("id") int id, ReporteResena reporte) throws BusinessLogicException {
        reporte.setId(id);
        reporteBL.aceptar(reporte);
    }

    @PUT
    @Path("{id}/rechazar")
    public void rechazar(@PathParam("id") int id) throws BusinessLogicException {
        reporteBL.rechazar(id);
    }
}
