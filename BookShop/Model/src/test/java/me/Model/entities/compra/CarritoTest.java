package me.Model.entities.compra;

import me.Model.entities.catalogo.Libro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarritoTest {
    @Test
    void carritoNuevoNoTieneItemsNiTotales() {
        Carrito carrito = new Carrito();
        assertTrue(carrito.getItems().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(carrito.getTotal()));
        assertEquals(0, carrito.getCantidad());
    }

    @Test
    void agregaUnLibro() {
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro(1, "30.50"), 2);
        assertEquals(1, carrito.getItems().size());
        assertEquals(2, carrito.getCantidad());
        assertEquals(0, new BigDecimal("61.00").compareTo(carrito.getTotal()));
    }

    @Test
    void agregarMismoLibroAumentaSuCantidad() {
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro(1, "30.50"), 1);
        carrito.agregarItem(libro(1, "30.50"), 2);
        assertEquals(1, carrito.getItems().size());
        assertEquals(3, carrito.getCantidad());
        assertEquals(0, new BigDecimal("91.50").compareTo(carrito.getTotal()));
    }

    @Test
    void modificaCantidadYRecalculaTotal() {
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro(1, "30.50"), 1);
        carrito.modificarCantidad(1, 3);
        assertEquals(3, carrito.getCantidad());
        assertEquals(0, new BigDecimal("91.50").compareTo(carrito.getTotal()));
    }

    @Test
    void cantidadCeroEliminaLibro() {
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro(1, "30.50"), 1);
        carrito.modificarCantidad(1, 0);
        assertTrue(carrito.getItems().isEmpty());
        assertEquals(0, carrito.getCantidad());
        assertEquals(0, BigDecimal.ZERO.compareTo(carrito.getTotal()));
    }

    @Test
    void eliminaUnLibroSinAfectarOtro() {
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro(1, "30.50"), 1);
        carrito.agregarItem(libro(2, "10.00"), 2);
        carrito.eliminarItem(1);
        assertEquals(1, carrito.getItems().size());
        assertEquals(2, carrito.getItems().get(0).getLibro().getId());
        assertEquals(0, new BigDecimal("20.00").compareTo(carrito.getTotal()));
    }

    @Test
    void vaciarDejaLaListaYLosTotalesEnCero() {
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro(1, "30.50"), 1);
        carrito.vaciar();
        assertTrue(carrito.getItems().isEmpty());
        assertEquals(0, carrito.getCantidad());
        assertEquals(0, BigDecimal.ZERO.compareTo(carrito.getTotal()));
    }

    @Test
    void setItemsCopiaLaEstructuraDeLaLista() {
        Carrito carrito = new Carrito();
        List<Item> externos = new ArrayList<>();
        externos.add(new Item(1, libro(1, "10.00"), 1));
        carrito.setItems(externos);
        externos.clear();
        assertEquals(1, carrito.getItems().size());
        assertEquals(0, new BigDecimal("10.00").compareTo(carrito.getTotal()));
    }

    @Test
    void modificarUnItemCompartidoCambiaElTotalCalculado() {
        Carrito carrito = new Carrito();
        Item item = new Item(1, libro(1, "10.00"), 1);
        carrito.setItems(List.of(item));
        item.setCantidad(3);
        assertEquals(3, carrito.getCantidad());
        assertEquals(0, new BigDecimal("30.00").compareTo(carrito.getTotal()));
    }

    @Test
    void sumaMonetariaDeDiezYVeinteCentimosEsExacta() {
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro(1, "0.10"), 1);
        carrito.agregarItem(libro(2, "0.20"), 1);
        assertEquals(0, new BigDecimal("0.30").compareTo(carrito.getTotal()));
    }

    private Libro libro(int id, String precio) {
        Libro libro = new Libro();
        libro.setId(id);
        libro.setPrecioBase(new BigDecimal(precio));
        return libro;
    }
}
