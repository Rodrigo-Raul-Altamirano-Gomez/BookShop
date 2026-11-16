package me.Business.recomendacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import me.Model.dto.compra.VentaLibroDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;
import me.Model.entities.catalogo.*;
import static org.junit.jupiter.api.Assertions.*;

class AlgoritmosRecomendacionTest {

    static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 7, 12, 0);

    static Libro libro(int id, int categoria) {
        Libro l = new Libro();
        l.setId(id);
        l.setTitulo("Libro " + id);
        l.setIdioma("Español");
        l.setSinopsis("Una historia");
        l.setProveedor("Distribuidora");
        l.setPrecioBase(new BigDecimal("20.00"));
        l.setFechaCreacion(AHORA.minusDays(10));
        l.setNumeroPaginas(100);
        Autor a = new Autor();
        a.setId(id);
        l.setAutor(a);
        Categoria c = new Categoria();
        c.setId(categoria);
        l.setCategoria(c);
        Editorial e = new Editorial();
        e.setId(id);
        l.setEditorial(e);
        return l;
    }

    static VentaLibroDTO venta(int pedido, int usuario, int libro, int cantidad) {
        return new VentaLibroDTO(pedido, usuario, libro, cantidad, AHORA.minusDays(1));
    }

    @Test
    void similitudUsaAtributosYExcluyeReferencia() {
        Libro origen = libro(1, 10);
        Libro coincide = libro(2, 10);
        Libro distinto = libro(3, 20);
        Map<Integer, Double> puntos = SimilitudContenido.puntuar(origen, List.of(origen, coincide, distinto));
        assertFalse(puntos.containsKey(1));
        assertTrue(puntos.get(2) > puntos.get(3));
    }

    @Test
    void asociacionCalculaConfianzaYLiftPorPedidoSinContarEjemplaresComoCompras() {
        var ventas = List.of(venta(1, 1, 1, 4), venta(1, 1, 2, 1), venta(2, 2, 1, 1), venta(2, 2, 2, 3), venta(3, 3, 1, 1), venta(3, 3, 3, 1), venta(4, 4, 3, 1));
        var puntos = AsociacionCompras.puntuar(Set.of(1), ventas);
        assertEquals(8.0 / 9, puntos.get(2), 0.000001);
        assertFalse(puntos.containsKey(1));
        assertFalse(puntos.containsKey(3));
    }

    @Test
    void colaborativoUsaJaccardYExcluyeComprados() {
        var ventas = List.of(venta(1, 1, 1, 1), venta(2, 1, 2, 1), venta(3, 2, 1, 1), venta(3, 2, 2, 1), venta(3, 2, 3, 1), venta(4, 3, 1, 1), venta(4, 3, 4, 1));
        var puntos = FiltradoColaborativo.puntuar(1, ventas);
        assertEquals(2.0 / 3, puntos.get(3), 0.000001);
        assertEquals(1.0 / 3, puntos.get(4), 0.000001);
        assertFalse(puntos.containsKey(1));
        assertFalse(puntos.containsKey(2));
    }

    @Test
    void popularidadIgnoraVentasFueraDeVentanaYNoInventaPromedioPublico() {
        Libro primero = libro(1, 10), segundo = libro(2, 10);
        var ventas = List.of(
            new VentaLibroDTO(1, 1, 1, 1000, AHORA.minusDays(91)),
            new VentaLibroDTO(2, 1, 1, 1000, AHORA.plusDays(1)),
            new VentaLibroDTO(3, 2, 2, 1, AHORA.minusDays(90)));
        var puntos = Popularidad.puntuar(List.of(primero, segundo), ventas, List.of(), AHORA);
        assertTrue(puntos.get(2) > puntos.get(1));
        assertTrue(Double.isFinite(puntos.get(1)));
    }

    @Test
    void popularidadCombinaNovedadYValoracionesAjustadasPorCantidadDeResenas() {
        Libro antiguo = libro(1, 10), nuevo = libro(2, 10), tercero = libro(3, 10);
        antiguo.setFechaCreacion(AHORA.minusDays(100));
        var novedad = Popularidad.puntuar(List.of(antiguo, nuevo), List.of(), List.of(), AHORA);
        assertTrue(novedad.get(2) > novedad.get(1));

        antiguo.setFechaCreacion(nuevo.getFechaCreacion());
        var valoraciones = List.of(
            new ValoracionLibroDTO(1, 0, 0, 0, 0, 1),
            new ValoracionLibroDTO(2, 0, 0, 0, 10, 0),
            new ValoracionLibroDTO(3, 100, 0, 0, 0, 0));
        var puntos = Popularidad.puntuar(List.of(antiguo, nuevo, tercero), List.of(), valoraciones, AHORA);
        // Una sola reseña de cinco estrellas se acerca al promedio global; no equivale a muchas reseñas.
        assertTrue(puntos.get(2) > puntos.get(1));
        assertTrue(puntos.get(1) > puntos.get(3));
    }
}
