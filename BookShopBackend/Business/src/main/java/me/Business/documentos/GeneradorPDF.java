package me.Business.documentos;

import java.io.InputStream;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.Business.exceptions.NegocioException;
import me.Model.dto.catalogo.ReporteVentasLibrosDTO;
import me.Model.dto.catalogo.ResumenVentasLibroDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.compra.ComprobanteCompraDTO;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.DireccionEnvioDTO;
import me.Model.dto.compra.ItemDTO;
import me.Model.dto.compra.MetodoPagoDTO;
import me.Model.dto.compra.ResumenCompraDTO;
import me.Model.dto.usuario.ReporteUsuariosDTO;
import me.Model.dto.usuario.ResumenActividadUsuarioDTO;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.compra.TipoTarjeta;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

// Prepara los parámetros y filas de cada JRXML y genera el PDF al solicitarlo.
public final class GeneradorPDF {
    private static final Locale PERU = Locale.of("es", "PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PERU);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PERU);
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PERU);

    public byte[] comprobante(ComprobanteCompraDTO comprobante) throws NegocioException {
        DetallePedidoDTO detalle = comprobante.pedido();
        ResumenCompraDTO resumen = detalle.resumen();
        EstadoPedido estado = detalle.pedido().estado();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.REPORT_LOCALE, PERU);
        parametros.put("fechaGeneracion", comprobante.fechaGeneracion().format(FECHA_HORA));
        parametros.put("numeroPedido", detalle.pedido().id());
        parametros.put("fechaPago", detalle.pedido().fechaPago().format(FECHA_HORA));
        parametros.put("estado", estado(estado));
        parametros.put("comprador", comprobante.comprador().nombre());
        parametros.put("correo", comprobante.comprador().email());
        parametros.put("transaccion", detalle.identificadorTransaccion());

        DireccionEnvioDTO direccion = detalle.direccionEnvio();
        String textoDireccion = direccion.calle() + ", " + direccion.distrito() + ", " + direccion.ciudad();
        if (direccion.alias() != null && !direccion.alias().isBlank())
            textoDireccion = direccion.alias() + "\n" + textoDireccion;
        if (direccion.referencia() != null && !direccion.referencia().isBlank())
            textoDireccion += "\nReferencia: " + direccion.referencia();
        parametros.put("direccion", textoDireccion);

        MetodoPagoDTO metodo = detalle.metodoPago();
        String tipo = "Billetera digital";
        if (metodo.tarjetaBancaria())
            tipo = "Tarjeta de " + (metodo.tipoTarjeta() == TipoTarjeta.DEBITO ? "débito" : "crédito");
        parametros.put("metodoPago", tipo + " " + metodo.proveedor() + " " + metodo.numeroEnmascarado()
            + "\n" + metodo.titular());

        String seguimiento = "Sin seguimiento: pedido " + estado(estado).toLowerCase(PERU) + ".";
        if (estado != EstadoPedido.CANCELADO && estado != EstadoPedido.DEVUELTO) {
            seguimiento = "Código: " + detalle.codigoSeguimiento();
            if (detalle.fechaEntregaEstimada() != null)
                seguimiento += "\nEntrega estimada: " + detalle.fechaEntregaEstimada().format(FECHA);
            if (detalle.fechaSalida() != null)
                seguimiento += "\nSalida: " + detalle.fechaSalida().format(FECHA_HORA);
            if (detalle.fechaEntrega() != null)
                seguimiento += "\nEntregado: " + detalle.fechaEntrega().format(FECHA_HORA);
        }
        parametros.put("seguimiento", seguimiento);
        parametros.put("subtotalProductos", resumen.subtotalProductos());
        parametros.put("descuentoLibros", resumen.descuentoLibros());
        parametros.put("subtotal", resumen.subtotal());
        parametros.put("etiquetaCupon", resumen.idCupon() == 0 ? "Cupón" :
            "Cupón (" + resumen.porcentajeCupon().stripTrailingZeros().toPlainString() + " %)");
        parametros.put("descuentoCupon", resumen.descuentoCupon());
        parametros.put("costoEnvio", resumen.costoEnvio());
        parametros.put("total", resumen.total());
        List<Map<String, ?>> filas = new ArrayList<>();
        for (ItemDTO item : resumen.items()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("titulo", item.titulo());
            fila.put("cantidad", item.cantidad());
            fila.put("precioOriginal", item.precioOriginal());
            fila.put("porcentajeDescuento", item.porcentajeDescuento());
            fila.put("subtotal", item.subtotal());
            filas.add(fila);
        }
        return generar("ComprobanteCompra", parametros, filas);
    }

    public byte[] ventasLibros(ReporteVentasLibrosDTO reporte) throws NegocioException {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.REPORT_LOCALE, PERU);
        parametros.put("fechaGeneracion", reporte.fechaGeneracion().format(FECHA_HORA));
        parametros.put("periodo", reporte.fechaInicio().format(FECHA) + " al " + reporte.fechaFin().format(FECHA));
        parametros.put("limite", reporte.cantidadMaxima() == null ? "Sin límite" : reporte.cantidadMaxima().toString());
        parametros.put("cantidadResultados", reporte.cantidadResultados());
        List<Map<String, ?>> filas = new ArrayList<>();
        int posicion = 1;
        for (ResumenVentasLibroDTO libro : reporte.resultados()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("posicion", posicion++);
            fila.put("titulo", libro.titulo());
            fila.put("categoria", libro.categoria());
            fila.put("autor", libro.autor());
            fila.put("editorial", libro.editorial());
            fila.put("cantidadVendida", libro.cantidadVendida());
            fila.put("importeVentas", libro.importeVentas());
            filas.add(fila);
        }
        return generar("ReporteVentasLibros", parametros, filas);
    }

    public byte[] usuarios(ReporteUsuariosDTO reporte) throws NegocioException {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.REPORT_LOCALE, PERU);
        parametros.put("fechaGeneracion", reporte.fechaGeneracion().format(FECHA_HORA));
        parametros.put("mes", reporte.mes().atDay(1).format(MES));
        parametros.put("limite", reporte.cantidadMaxima() == null ? "Sin límite" : reporte.cantidadMaxima().toString());
        parametros.put("cantidadResultados", reporte.cantidadResultados());
        List<Map<String, ?>> filas = new ArrayList<>();
        int posicion = 1;
        for (ResumenActividadUsuarioDTO actividad : reporte.resultados()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("posicion", posicion++);
            fila.put("nombre", actividad.usuario().nombre());
            fila.put("email", actividad.usuario().email());
            fila.put("estado", actividad.activo() ? "Activo" : "Inactivo");
            fila.put("totalGastado", actividad.totalGastado());
            Duration tiempo = actividad.tiempoTotal();
            fila.put("tiempoUso", String.format("%d h %02d min", tiempo.toHours(), tiempo.toMinutesPart()));
            fila.put("categoria", nombre(actividad.categoriaMasComprada()));
            fila.put("autor", nombre(actividad.autorMasComprado()));
            fila.put("editorial", nombre(actividad.editorialMasComprada()));
            filas.add(fila);
        }
        return generar("ReporteUsuarios", parametros, filas);
    }

    private static byte[] generar(String plantilla, Map<String, Object> parametros, List<Map<String, ?>> filas)
        throws NegocioException {
        try (InputStream jrxml = GeneradorPDF.class.getResourceAsStream("/reportes/" + plantilla + ".jrxml")) {
            JasperReport reporte = JasperCompileManager.compileReport(jrxml);
            JasperPrint impresion = JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(filas));
            return JasperExportManager.exportReportToPdf(impresion);
        } catch (Exception e) {
            throw new NegocioException("No se pudo generar el documento PDF.", e);
        }
    }

    private static String estado(EstadoPedido estado) {
        return switch (estado) {
            case EN_ESPERA -> "En espera";
            case EN_ALMACEN -> "En almacén";
            case EN_CAMINO -> "En camino";
            case ENTREGADO -> "Entregado";
            case CANCELADO -> "Cancelado";
            case DEVUELTO -> "Devuelto";
        };
    }

    private static String nombre(TarjetaReferenciaDTO referencia) {
        return referencia == null ? "Sin compras" : referencia.nombre();
    }
}
