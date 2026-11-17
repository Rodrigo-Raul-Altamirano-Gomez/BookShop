package me.Service.documentos;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Business.documentos.ComprobanteBO;
import me.Business.documentos.ComprobanteBOImpl;
import me.Business.documentos.ReporteBO;
import me.Business.documentos.ReporteBOImpl;
import me.Service.Sesion;

// RF17, RF18 y RF19: documentos PDF generados con JasperReports.
@Path("documentos")
@Produces("application/pdf")
public class ReporteRS {
    private final ComprobanteBO comprobanteBO = new ComprobanteBOImpl();
    private final ReporteBO reporteBO = new ReporteBOImpl();
    @Context
    private HttpServletRequest peticion;

    // RF17: comprobante de un pedido del usuario en sesión.
    @GET
    @Path("comprobantes/{idPedido}")
    public Response comprobante(@PathParam("idPedido") int idPedido) throws NegocioException {
        byte[] pdf = comprobanteBO.generarPDF(Sesion.exigirIdUsuario(peticion), idPedido);
        return pdf(pdf, "comprobante-pedido-" + idPedido + ".pdf");
    }

    // RF18: libros ordenados por monto vendido. Fechas ISO (aaaa-mm-dd); cantidad vacía significa sin límite.
    @GET
    @Path("ventas-libros")
    public Response ventasLibros(@QueryParam("inicio") String inicio, @QueryParam("fin") String fin,
            @QueryParam("cantidad") Integer cantidad) throws NegocioException {
        ConsultaReporteLibrosDTO consulta = new ConsultaReporteLibrosDTO(fecha("inicio", inicio), fecha("fin", fin), cantidad);
        Sesion.exigirIdUsuario(peticion);
        byte[] pdf = reporteBO.generarLibrosPDF(consulta);
        return pdf(pdf, "reporte-ventas-libros-" + inicio + "-" + fin + ".pdf");
    }

    // RF19: usuarios ordenados por gasto del mes (aaaa-mm).
    @GET
    @Path("usuarios")
    public Response usuarios(@QueryParam("mes") String mes, @QueryParam("cantidad") Integer cantidad)
            throws NegocioException {
        YearMonth periodo;
        try {
            periodo = mes == null || mes.isBlank() ? null : YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw new ValidacionException("mes", "Selecciona un mes y año válidos.");
        }
        Sesion.exigirIdUsuario(peticion);
        byte[] pdf = reporteBO.generarUsuariosPDF(periodo, cantidad);
        return pdf(pdf, "reporte-usuarios-" + mes + ".pdf");
    }

    private static LocalDate fecha(String campo, String valor) throws ValidacionException {
        try {
            return valor == null || valor.isBlank() ? null : LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new ValidacionException(campo, "La fecha no es válida.");
        }
    }

    private static Response pdf(byte[] contenido, String nombreArchivo) {
        return Response.ok(contenido, "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                .build();
    }
}
