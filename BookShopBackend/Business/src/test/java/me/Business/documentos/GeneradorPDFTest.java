package me.Business.documentos;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import me.Model.dto.catalogo.ReporteVentasLibrosDTO;
import me.Model.dto.catalogo.ResumenVentasLibroDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.compra.*;
import me.Model.dto.usuario.ReporteUsuariosDTO;
import me.Model.dto.usuario.ResumenActividadUsuarioDTO;
import me.Model.dto.usuario.TarjetaUsuarioDTO;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.compra.TipoTarjeta;
import static org.junit.jupiter.api.Assertions.*;

class GeneradorPDFTest {
    private static final LocalDateTime GENERACION = LocalDateTime.of(2026, 11, 10, 18, 45);
    private final GeneradorPDF generador = new GeneradorPDF();

    private static BigDecimal s(String valor) { return new BigDecimal(valor); }

    private static void verificarPDF(byte[] pdf, String nombre) throws Exception {
        assertNotNull(pdf);
        assertTrue(pdf.length > 1000, "El PDF está vacío");
        assertEquals("%PDF", new String(pdf, 0, 4));
        // Copia de revisión visual: target/reportes-prueba/<nombre>.pdf
        Path carpeta = Path.of("target", "reportes-prueba");
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve(nombre + ".pdf"), pdf);
    }

    @Test void plantillasCompilan() throws Exception {
        assertNotNull(GeneradorPDF.compilar(GeneradorPDF.COMPROBANTE));
        assertNotNull(GeneradorPDF.compilar(GeneradorPDF.VENTAS_LIBROS));
        assertNotNull(GeneradorPDF.compilar(GeneradorPDF.USUARIOS));
    }

    @Test void comprobanteDeCompraRF17() throws Exception {
        var items = List.of(
            new ItemDTO(1, "Cien años de soledad", null, 2, s("59.90"), s("50.92"), s("15"), s("101.84"), null, null),
            new ItemDTO(2, "La ciudad y los perros", null, 1, s("45.00"), s("45.00"), s("0"), s("45.00"), null, null));
        var resumen = new ResumenCompraDTO(items, s("164.80"), s("17.96"), 3, s("10"), s("14.68"),
            s("5"), s("6.61"), s("138.77"), 3);
        var detalle = new DetallePedidoDTO(
            new ResumenPedidoDTO(40, LocalDateTime.of(2026, 11, 8, 10, 15), EstadoPedido.EN_CAMINO, 3, s("138.77")),
            resumen,
            new DireccionEnvioDTO(1, "Casa", "Av. Universitaria 1801", "San Miguel", "Lima", "Frente al parque", true),
            new MetodoPagoDTO(1, true, "Ana Pérez", "Visa", "Personal", "•••• 4242", TipoTarjeta.CREDITO,
                LocalDate.of(2028, 5, 31), true, true),
            "TX-20261108-0040", "BS-40-ABCD", LocalDateTime.of(2026, 11, 12, 18, 0),
            LocalDateTime.of(2026, 11, 9, 9, 0), null, false, false);
        var dto = new ComprobanteCompraDTO(detalle, new TarjetaUsuarioDTO(1, "Ana Pérez", "ana@mail.com", null), GENERACION);
        verificarPDF(generador.comprobante(dto), "ComprobanteCompra");
    }

    @Test void comprobanteCanceladoSinCuponNiSeguimiento() throws Exception {
        var resumen = new ResumenCompraDTO(List.of(), s("0"), s("0"), 0, null, null, s("0"), s("0"), s("0"), 0);
        var detalle = new DetallePedidoDTO(
            new ResumenPedidoDTO(41, LocalDateTime.of(2026, 11, 8, 10, 15), EstadoPedido.CANCELADO, 0, s("0")),
            resumen, null, new MetodoPagoDTO(2, false, null, "Yape", null, "••• 789", null, null, false, true),
            null, null, null, null, null, false, false);
        var dto = new ComprobanteCompraDTO(detalle, new TarjetaUsuarioDTO(1, "Ana", "ana@mail.com", null), GENERACION);
        verificarPDF(generador.comprobante(dto), "ComprobanteCancelado");
    }

    @Test void reporteVentasLibrosRF18() throws Exception {
        var filas = List.of(
            new ResumenVentasLibroDTO(1, "Cien años de soledad", "Novela", "Gabriel García Márquez", "Sudamericana", 12, s("611.04")),
            new ResumenVentasLibroDTO(2, "La ciudad y los perros", "Novela", "Mario Vargas Llosa", "Alfaguara", 7, s("315.00")),
            new ResumenVentasLibroDTO(3, "Sin autor", null, null, null, 1, s("20.00")));
        var dto = new ReporteVentasLibrosDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), GENERACION, 10, filas);
        verificarPDF(generador.ventasLibros(dto), "ReporteVentasLibros");
        var vacio = new ReporteVentasLibrosDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), GENERACION, null, List.of());
        verificarPDF(generador.ventasLibros(vacio), "ReporteVentasLibrosVacio");
    }

    @Test void reporteUsuariosRF19() throws Exception {
        var filas = List.of(
            new ResumenActividadUsuarioDTO(new TarjetaUsuarioDTO(1, "Ana Pérez", "ana@mail.com", null), true, s("250.40"),
                Duration.ofMinutes(185), new TarjetaReferenciaDTO(1, "Novela", null),
                new TarjetaReferenciaDTO(2, "Gabriel García Márquez", null), new TarjetaReferenciaDTO(3, "Sudamericana", null)),
            new ResumenActividadUsuarioDTO(new TarjetaUsuarioDTO(2, "Luis Rojas", "luis@mail.com", null), false, s("0"),
                Duration.ZERO, null, null, null));
        var dto = new ReporteUsuariosDTO(YearMonth.of(2026, 10), GENERACION, null, filas);
        verificarPDF(generador.usuarios(dto), "ReporteUsuarios");
    }

    @Test void formatosAuxiliares() {
        assertEquals("3 h 05 min", GeneradorPDF.tiempo(Duration.ofMinutes(185)));
        assertEquals("0 h 00 min", GeneradorPDF.tiempo(null));
        assertEquals("En almacén", GeneradorPDF.estado(EstadoPedido.EN_ALMACEN));
        assertEquals("Cancelado", GeneradorPDF.estado(EstadoPedido.CANCELADO));
    }
}
