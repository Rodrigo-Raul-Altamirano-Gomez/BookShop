package me.Model.entities.compra;

import me.Model.entities.catalogo.Libro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTest {
    @Test
    void calculaSubtotalAlConstruirse() {
        Item item = new Item(1, libro("30.50"), 2);
        assertEquals(0, new BigDecimal("30.50").compareTo(item.getPrecioUnitario()));
        assertEquals(0, new BigDecimal("61.00").compareTo(item.getSubTotal()));
    }

    @Test
    void recalculaSubtotalAlCambiarCantidad() {
        Item item = new Item(1, libro("30.50"), 2);
        item.setCantidad(3);
        assertEquals(0, new BigDecimal("91.50").compareTo(item.getSubTotal()));
    }

    @Test
    void recalculaSubtotalAlCambiarPrecio() {
        Item item = new Item(1, libro("30.50"), 2);
        item.setPrecioUnitario(new BigDecimal("12.25"));
        assertEquals(0, new BigDecimal("24.50").compareTo(item.getSubTotal()));
    }

    @Test
    void conservaCalculoMonetarioExacto() {
        Item item = new Item(1, libro("0.10"), 3);
        assertEquals(0, new BigDecimal("0.30").compareTo(item.getSubTotal()));
        item.setPrecioUnitario(null);
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getSubTotal()));
    }

    @Test
    void constructorVacioInicializaSubtotalEnCero() {
        Item item = new Item();
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getSubTotal()));
    }

    private Libro libro(String precio) {
        Libro libro = new Libro();
        libro.setPrecioBase(new BigDecimal(precio));
        return libro;
    }
}
