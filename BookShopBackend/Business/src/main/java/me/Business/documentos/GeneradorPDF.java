package me.Business.documentos;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.Business.exceptions.ConfiguracionException;
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
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

// Convierte los DTO preparados por negocio en los parametros y filas de las plantillas JRXML.
// Las plantillas se compilan una sola vez y se reutilizan en las siguientes solicitudes.
public final class GeneradorPDF {
    static final String COMPROBANTE = "ComprobanteCompra";
    static final String VENTAS_LIBROS = "ReporteVentasLibros";
    static final String USUARIOS = "ReporteUsuarios";
    private static final Locale PERU = Locale.of("es", "PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PERU);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PERU);
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PERU);
    private static final String SIN_DATO = "—";
    private static final Map<String, JasperReport> COMPILADOS = new ConcurrentHashMap<>();

    public byte[] comprobante(ComprobanteCompraDTO comprobante) throws ConfiguracionException {
        DetallePedidoDTO detalle = comprobante.pedido();
        ResumenCompraDTO resumen = detalle.resumen();
        EstadoPedido estado = detalle.pedido().estado();
        Map<String, Object> parametros = parametrosBase(comprobante.fechaGeneracion());
        parametros.put("numeroPedido", detalle.pedido().id());
        parametros.put("fechaPago", fechaHora(detalle.pedido().fechaPago()));
        parametros.put("estado", estado(estado));
        parametros.put("comprador", texto(comprobante.comprador().nombre()));
        parametros.put("correo", texto(comprobante.comprador().email()));
        parametros.put("direccion", direccion(detalle.direccionEnvio()));
        parametros.put("metodoPago", metodoPago(detalle.metodoPago()));
        parametros.put("transaccion", texto(detalle.identificadorTransaccion()));
        parametros.put("seguimiento", seguimiento(detalle, estado));
        parametros.put("subtotalProductos", importe(resumen.subtotalProductos()));
        parametros.put("descuentoLibros", importe(resumen.descuentoLibros()));
        parametros.put("etiquetaCupon", resumen.idCupon() > 0 && resumen.porcentajeCupon() != null
            ? "Cupón (" + resumen.porcentajeCupon().stripTrailingZeros().toPlainString() + " %)" : "Cupón");
        parametros.put("descuentoCupon", importe(resumen.descuentoCupon()));
        parametros.put("costoEnvio", importe(resumen.costoEnvio()));
        parametros.put("total", importe(resumen.total()));
        List<Map<String, ?>> filas = new ArrayList<>();
        for (ItemDTO item : resumen.items()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("titulo", item.titulo());
            fila.put("cantidad", item.cantidad());
            fila.put("precioOriginal", importe(item.precioOriginal()));
            fila.put("porcentajeDescuento", importe(item.porcentajeDescuento()));
            fila.put("precioUnitario", importe(item.precioUnitario()));
            fila.put("subtotal", importe(item.subtotal()));
            filas.add(fila);
        }
        return generar(COMPROBANTE, parametros, filas);
    }

    public byte[] ventasLibros(ReporteVentasLibrosDTO reporte) throws ConfiguracionException {
        Map<String, Object> parametros = parametrosBase(reporte.fechaGeneracion());
        parametros.put("periodo", fecha(reporte.fechaInicio()) + " al " + fecha(reporte.fechaFin()));
        parametros.put("limite", limite(reporte.cantidadMaxima()));
        parametros.put("cantidadResultados", reporte.cantidadResultados());
        List<Map<String, ?>> filas = new ArrayList<>();
        int posicion = 1;
        for (ResumenVentasLibroDTO libro : reporte.resultados()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("posicion", posicion++);
            fila.put("titulo", libro.titulo());
            fila.put("categoria", texto(libro.categoria()));
            fila.put("autor", texto(libro.autor()));
            fila.put("editorial", texto(libro.editorial()));
            fila.put("cantidadVendida", libro.cantidadVendida());
            fila.put("importeVentas", importe(libro.importeVentas()));
            filas.add(fila);
        }
        return generar(VENTAS_LIBROS, parametros, filas);
    }

    public byte[] usuarios(ReporteUsuariosDTO reporte) throws ConfiguracionException {
        Map<String, Object> parametros = parametrosBase(reporte.fechaGeneracion());
        String mes = reporte.mes().atDay(1).format(MES);
        parametros.put("mes", Character.toUpperCase(mes.charAt(0)) + mes.substring(1));
        parametros.put("limite", limite(reporte.cantidadMaxima()));
        parametros.put("cantidadResultados", reporte.cantidadResultados());
        List<Map<String, ?>> filas = new ArrayList<>();
        int posicion = 1;
        for (ResumenActividadUsuarioDTO actividad : reporte.resultados()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("posicion", posicion++);
            fila.put("nombre", texto(actividad.usuario().nombre()));
            fila.put("email", texto(actividad.usuario().email()));
            fila.put("estado", actividad.activo() ? "Activo" : "Inactivo");
            fila.put("totalGastado", importe(actividad.totalGastado()));
            fila.put("tiempoUso", tiempo(actividad.tiempoTotal()));
            fila.put("categoria", nombre(actividad.categoriaMasComprada()));
            fila.put("autor", nombre(actividad.autorMasComprado()));
            fila.put("editorial", nombre(actividad.editorialMasComprada()));
            filas.add(fila);
        }
        return generar(USUARIOS, parametros, filas);
    }

    private static byte[] generar(String plantilla, Map<String, Object> parametros, List<Map<String, ?>> filas)
            throws ConfiguracionException {
        try {
            var impresion = JasperFillManager.fillReport(compilar(plantilla), parametros,
                new JRMapCollectionDataSource(filas));
            return JasperExportManager.exportReportToPdf(impresion);
        } catch (JRException | RuntimeException e) {
            throw new ConfiguracionException("No se pudo generar el documento PDF.", e);
        }
    }

    static JasperReport compilar(String plantilla) throws ConfiguracionException {
        JasperReport compilado = COMPILADOS.get(plantilla);
        if (compilado != null) return compilado;
        try (InputStream jrxml = GeneradorPDF.class.getResourceAsStream("/reportes/" + plantilla + ".jrxml")) {
            if (jrxml == null) throw new ConfiguracionException("No se encontró la plantilla " + plantilla + ".");
            compilado = JasperCompileManager.compileReport(jrxml);
        } catch (ConfiguracionException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfiguracionException("No se pudo compilar la plantilla " + plantilla + ".", e);
        }
        COMPILADOS.putIfAbsent(plantilla, compilado);
        return compilado;
    }

    private static Map<String, Object> parametrosBase(LocalDateTime fechaGeneracion) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.REPORT_LOCALE, PERU);
        parametros.put("fechaGeneracion", fechaHora(fechaGeneracion));
        return parametros;
    }

    static String estado(EstadoPedido estado) {
        if (estado == null) return SIN_DATO;
        return switch (estado) {
            case EN_ESPERA -> "En espera";
            case EN_ALMACEN -> "En almacén";
            case EN_CAMINO -> "En camino";
            case ENTREGADO -> "Entregado";
            case CANCELADO -> "Cancelado";
            case DEVUELTO -> "Devuelto";
        };
    }

    private static String direccion(DireccionEnvioDTO direccion) {
        if (direccion == null) return SIN_DATO;
        StringBuilder texto = new StringBuilder();
        if (direccion.alias() != null && !direccion.alias().isBlank()) texto.append(direccion.alias()).append('\n');
        texto.append(String.join(", ", noVacios(direccion.calle(), direccion.distrito(), direccion.ciudad())));
        if (direccion.referencia() != null && !direccion.referencia().isBlank())
            texto.append("\nReferencia: ").append(direccion.referencia());
        return texto.toString();
    }

    private static String metodoPago(MetodoPagoDTO metodo) {
        if (metodo == null) return SIN_DATO;
        String tipo;
        if (metodo.tarjetaBancaria())
            tipo = "Tarjeta de " + (metodo.tipoTarjeta() == TipoTarjeta.DEBITO ? "débito" : "crédito");
        else tipo = "Billetera digital";
        String linea = String.join(" ", noVacios(tipo, metodo.proveedor(), metodo.numeroEnmascarado()));
        return metodo.titular() == null || metodo.titular().isBlank() ? linea : linea + "\n" + metodo.titular();
    }

    private static String seguimiento(DetallePedidoDTO detalle, EstadoPedido estado) {
        if (estado == EstadoPedido.CANCELADO || estado == EstadoPedido.DEVUELTO)
            return "Sin seguimiento: pedido " + estado(estado).toLowerCase(PERU) + ".";
        List<String> lineas = new ArrayList<>();
        lineas.add("Código: " + texto(detalle.codigoSeguimiento()));
        if (detalle.fechaEntregaEstimada() != null)
            lineas.add("Entrega estimada: " + detalle.fechaEntregaEstimada().format(FECHA));
        if (detalle.fechaSalida() != null) lineas.add("Salida: " + fechaHora(detalle.fechaSalida()));
        if (detalle.fechaEntrega() != null) lineas.add("Entregado: " + fechaHora(detalle.fechaEntrega()));
        return String.join("\n", lineas);
    }

    static String tiempo(Duration duracion) {
        if (duracion == null || duracion.isNegative()) duracion = Duration.ZERO;
        return duracion.toHours() + " h " + String.format("%02d", duracion.toMinutesPart()) + " min";
    }

    private static String limite(Integer cantidadMaxima) {
        return cantidadMaxima == null ? "Sin límite" : cantidadMaxima.toString();
    }

    private static String nombre(TarjetaReferenciaDTO referencia) {
        return referencia == null ? SIN_DATO : texto(referencia.nombre());
    }

    private static List<String> noVacios(String... valores) {
        List<String> resultado = new ArrayList<>();
        for (String valor : valores) if (valor != null && !valor.isBlank()) resultado.add(valor);
        return resultado;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? SIN_DATO : valor;
    }

    private static BigDecimal importe(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static String fecha(LocalDate fecha) {
        return fecha == null ? SIN_DATO : fecha.format(FECHA);
    }

    private static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? SIN_DATO : fecha.format(FECHA_HORA);
    }
}
