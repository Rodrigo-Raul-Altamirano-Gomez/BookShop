package pe.edu.pucp.bookshop.rs.reporte;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.reporte.ReporteBL;
import pe.edu.pucp.bookshop.bl.reporte.impl.ReporteBLImpl;

// Reportes PDF del administrador; se abren en el navegador (inline)
@Path("reportes")
@Produces("application/pdf")
public class ReporteRS {

    private ReporteBL reporteBL = new ReporteBLImpl();

    @GET
    @Path("libros")
    public Response generarReporteLibros(@BeanParam ParametrosReporte parametros) throws BusinessLogicException {
        byte[] pdf = reporteBL.generarReporteLibros(parametros.getDesde(), parametros.getHasta(), parametros.getLimite());
        return Response.ok(pdf).header("Content-Disposition", "inline; filename=\"libros-por-monto-vendido.pdf\"").build();
    }

    @GET
    @Path("clientes")
    public Response generarReporteClientes(@BeanParam ParametrosReporte parametros) throws BusinessLogicException {
        byte[] pdf = reporteBL.generarReporteClientes(parametros.getDesde(), parametros.getHasta(), parametros.getLimite());
        return Response.ok(pdf).header("Content-Disposition", "inline; filename=\"clientes-por-monto-gastado.pdf\"").build();
    }
}
