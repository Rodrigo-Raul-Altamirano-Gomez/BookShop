package me.Business.documentos;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import me.Model.dto.catalogo.ReporteVentasLibrosDTO;
import me.Model.dto.catalogo.ResumenVentasLibroDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.compra.*;
import me.Model.dto.usuario.ReporteUsuariosDTO;
import me.Model.dto.usuario.ResumenActividadUsuarioDTO;
import me.Model.dto.usuario.TarjetaUsuarioDTO;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.compra.TipoTarjeta;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneradorPDFTest {
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 10, 18, 45);
    private final GeneradorPDF generador = new GeneradorPDF();

    private static BigDecimal monto(String valor) {
        return new BigDecimal(valor);
    }

    private static PdfReader abrirPDF(byte[] datos, String nombre) throws Exception {
        assertEquals("%PDF", new String(datos, 0, 4, StandardCharsets.US_ASCII));
        Path carpeta = Path.of("target", "reportes-prueba");
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve(nombre + ".pdf"), datos);
        return new PdfReader(datos);
    }

    private ComprobanteCompraDTO comprobante(EstadoPedido estado) {
        var items = List.of(
            new ItemDTO(1, "Cien años de soledad", null, 2, monto("59.90"), monto("50.92"),
                monto("15"), monto("101.84"), null, null),
            new ItemDTO(2, "La ciudad y los perros", null, 1, monto("45.00"), monto("45.00"),
                BigDecimal.ZERO, monto("45.00"), null, null));
        var resumen = new ResumenCompraDTO(items, monto("164.80"), monto("17.96"), 3,
            monto("10"), monto("14.68"), monto("5"), monto("7.34"), monto("139.50"), 3);
        var detalle = new DetallePedidoDTO(
            new ResumenPedidoDTO(40, FECHA.minusDays(2), estado, 3, monto("139.50")), resumen,
            new DireccionEnvioDTO(1, "Casa", "Av. Universitaria 1801", "San Miguel", "Lima",
                "Frente al parque", true),
            new MetodoPagoDTO(1, true, "Ana Pérez", "Visa", "Personal", "**** 4242",
                TipoTarjeta.CREDITO, LocalDate.of(2028, 5, 31), true, true),
            "TX-40", "BS-40", FECHA.plusDays(1), FECHA.minusDays(1), FECHA, false, false);
        return new ComprobanteCompraDTO(detalle,
            new TarjetaUsuarioDTO(1, "Ana Pérez", "ana@mail.com", null), FECHA);
    }

    @Test
    void comprobanteConservaImportesHistoricosYDatosDelPedido() throws Exception {
        try (PdfReader pdf = abrirPDF(generador.comprobante(comprobante(EstadoPedido.ENTREGADO)),
                "ComprobanteCompra")) {
            String texto = new PdfTextExtractor(pdf).getTextFromPage(1);
            assertTrue(texto.contains("Ana Pérez") && texto.contains("ana@mail.com"));
            assertTrue(texto.contains("Av. Universitaria 1801") && texto.contains("San Miguel"));
            assertTrue(texto.contains("**** 4242") && texto.contains("TX-40"));
            assertTrue(texto.contains("Cien años de soledad") && texto.contains("La ciudad y los perros"));
            assertTrue(texto.contains("S/ 101.84") && texto.contains("S/ 139.50"));
            assertTrue(texto.contains("Productos originales") && texto.contains("Subtotal"));
            assertTrue(texto.contains("15 %") && texto.contains("—"));
            assertFalse(texto.contains("Precio unit."));
            assertTrue(texto.contains("BS-40") && texto.contains("Entregado:"));
            assertEquals(1, pdf.getNumberOfPages());
        }
    }

    @Test
    void canceladosYDevueltosConservanImportesSinSeguimiento() throws Exception {
        for (EstadoPedido estado : List.of(EstadoPedido.CANCELADO, EstadoPedido.DEVUELTO)) {
            try (PdfReader pdf = abrirPDF(generador.comprobante(comprobante(estado)), estado.name())) {
                String texto = new PdfTextExtractor(pdf).getTextFromPage(1);
                assertTrue(texto.contains("S/ 139.50"));
                assertFalse(texto.contains("BS-40"));
                assertFalse(texto.contains("Entrega estimada:"));
                assertFalse(texto.contains("Entregado:"));
            }
        }
    }

    @Test
    void reporteDeUsuariosIncluyeSinComprasYTiempoAcumulado() throws Exception {
        var filas = List.of(
            new ResumenActividadUsuarioDTO(new TarjetaUsuarioDTO(1, "Ana Pérez", "ana@mail.com", null),
                true, monto("250.40"), Duration.ofMinutes(185), new TarjetaReferenciaDTO(1, "Novela", null),
                new TarjetaReferenciaDTO(2, "Gabriel García Márquez", null),
                new TarjetaReferenciaDTO(3, "Sudamericana", null)),
            new ResumenActividadUsuarioDTO(new TarjetaUsuarioDTO(2, "Luis Rojas", "luis@mail.com", null),
                false, BigDecimal.ZERO, Duration.ZERO, null, null, null));
        var reporte = new ReporteUsuariosDTO(YearMonth.of(2026, 10), FECHA, null, filas);
        try (PdfReader pdf = abrirPDF(generador.usuarios(reporte), "ReporteUsuarios")) {
            String texto = new PdfTextExtractor(pdf).getTextFromPage(1);
            assertTrue(texto.contains("Ana Pérez") && texto.contains("Luis Rojas"));
            assertTrue(texto.contains("S/ 250.40") && texto.contains("S/ 0.00"));
            assertTrue(texto.contains("3 h 05 min") && texto.contains("Sin compras"));
            assertTrue(texto.contains("Sin límite") && texto.contains("octubre de 2026"));
        }
    }

    @Test
    void reportesVaciosConservanCabeceraYMensaje() throws Exception {
        var ventas = new ReporteVentasLibrosDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
            FECHA, null, List.of());
        try (PdfReader pdf = abrirPDF(generador.ventasLibros(ventas), "ReporteVentasLibrosVacio")) {
            String texto = new PdfTextExtractor(pdf).getTextFromPage(1);
            assertTrue(texto.contains("BookShop") && texto.contains("01/10/2026 al 31/10/2026"));
            assertTrue(texto.contains("No se registraron ventas válidas"));
        }
        var usuarios = new ReporteUsuariosDTO(YearMonth.of(2026, 10), FECHA, null, List.of());
        try (PdfReader pdf = abrirPDF(generador.usuarios(usuarios), "ReporteUsuariosVacio")) {
            String texto = new PdfTextExtractor(pdf).getTextFromPage(1);
            assertTrue(texto.contains("BookShop") && texto.contains("octubre de 2026"));
            assertTrue(texto.contains("No hay usuarios"));
        }
    }

    @Test
    void reporteLargoRepiteCabeceraYNumeracionSinCortarFilas() throws Exception {
        List<ResumenVentasLibroDTO> filas = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            String titulo = "Inicio-" + i + " Título largo para verificar la continuación de la tabla "
                + "y la conservación de cada fila entre páginas Fin-" + i;
            filas.add(new ResumenVentasLibroDTO(i, titulo, "Novela", "Gabriel García Márquez",
                "Sudamericana", 2, monto("101.84")));
        }
        var reporte = new ReporteVentasLibrosDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
            FECHA, 80, filas);
        try (PdfReader pdf = abrirPDF(generador.ventasLibros(reporte), "ReporteVentasLibros")) {
            assertTrue(pdf.getNumberOfPages() > 1);
            List<String> paginas = new ArrayList<>();
            for (int pagina = 1; pagina <= pdf.getNumberOfPages(); pagina++) {
                String texto = new PdfTextExtractor(pdf).getTextFromPage(pagina);
                assertTrue(texto.contains("Libro") && texto.contains("Monto vendido"));
                assertTrue(texto.contains("Página " + pagina + " de"));
                paginas.add(texto);
            }
            for (int i = 1; i <= 80; i++) {
                int apariciones = 0;
                for (String pagina : paginas) {
                    if (pagina.contains("Inicio-" + i + " ")) {
                        assertTrue(pagina.contains("Fin-" + i), "La fila " + i + " está cortada");
                        apariciones++;
                    }
                }
                assertEquals(1, apariciones, "Falta la fila " + i);
            }
        }
    }
}
