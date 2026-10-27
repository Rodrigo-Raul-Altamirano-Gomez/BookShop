package me.Model.entities.compra;

import me.Model.entities.catalogo.Libro;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.compra.DireccionEnvio;
import me.Model.entities.compra.TarjetaBancaria;
import me.Model.entities.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PedidoTest {
    @Test
    void constructorVacioInicializaItemsYTotal() {
        Pedido pedido = new Pedido();
        assertTrue(pedido.getItems().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(pedido.getTotal()));
    }

    @Test
    void constructorDesdeCarritoCopiaItemsTotalYCantidad() {
        Usuario usuario = new Usuario();
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro(1, "30.50"), 2);
        carrito.agregarItem(libro(2, "0.20"), 1);
        Pedido pedido = new Pedido(1, usuario, carrito, new DireccionEnvio());
        assertEquals(2, pedido.getItems().size());
        assertEquals(3, pedido.getCantidadTotalLibros());
        assertEquals(0, new BigDecimal("61.20").compareTo(pedido.getTotal()));
        carrito.vaciar();
        assertEquals(2, pedido.getItems().size());
    }

    @Test
    void setItemsNoModificaTotalNiCantidadHistorica() {
        Pedido pedido = new Pedido();
        pedido.setTotal(new BigDecimal("40.00"));
        pedido.setCantidadTotalLibros(4);
        List<Item> externos = new ArrayList<>();
        externos.add(new Item(1, libro(1, "10.00"), 1));
        pedido.setItems(externos);
        externos.clear();
        assertEquals(1, pedido.getItems().size());
        assertEquals(4, pedido.getCantidadTotalLibros());
        assertEquals(0, new BigDecimal("40.00").compareTo(pedido.getTotal()));
    }

    @Test
    void getItemsNoExponeLaEstructuraInterna() {
        Pedido pedido = new Pedido();
        pedido.setItems(List.of(new Item(1, libro(1, "10.00"), 1)));
        List<Item> devueltos = pedido.getItems();
        devueltos.clear();
        assertEquals(1, pedido.getItems().size());
    }

    @Test
    void pedidoEnEsperaNoSimulaUnPagoNiUnEnvio() {
        Pedido pedido = new Pedido();
        assertEquals(EstadoPedido.EN_ESPERA, pedido.getEstado());
        assertNull(pedido.getIdentificadorTransaccion());
        assertNull(pedido.getFechaPago());
        assertNull(pedido.getCodigoSeguimiento());
        assertNull(pedido.getFechaEntregaEstimada());
        assertNull(pedido.getDireccionEnvio());
        assertNull(pedido.getMetodoPago());
    }

    @Test
    void modificarCarritoOLibroNoAlteraLasLineasDelPedido() {
        Libro libro = libro(1, "50.00");
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro, 2);
        Item itemCarrito = carrito.getItems().getFirst();
        itemCarrito.setPrecioUnitario(new BigDecimal("40.00"));
        Pedido pedido = new Pedido(1, new Usuario(), carrito);

        carrito.modificarCantidad(1, 5);
        itemCarrito.setPrecioUnitario(new BigDecimal("30.00"));
        libro.setPrecioBase(new BigDecimal("60.00"));

        Item itemPedido = pedido.getItems().getFirst();
        assertEquals(2, itemPedido.getCantidad());
        assertEquals(0, new BigDecimal("50.00").compareTo(itemPedido.getPrecioBase()));
        assertEquals(0, new BigDecimal("40.00").compareTo(itemPedido.getPrecioUnitario()));
        assertEquals(0, new BigDecimal("100.00").compareTo(pedido.getSubtotalProductos()));
        assertEquals(0, new BigDecimal("20.00").compareTo(pedido.getDescuentoLibros()));
        assertEquals(0, new BigDecimal("80.00").compareTo(pedido.getSubtotal()));
        assertEquals(0, new BigDecimal("80.00").compareTo(pedido.getTotal()));
    }

    private Libro libro(int id, String precio) {
        Libro libro = new Libro();
        libro.setId(id);
        libro.setPrecioBase(new BigDecimal(precio));
        return libro;
    }

    @Test
    void compraConsultaDatosActualesSinDuplicarInformacionRelacionada() {
        Libro libro = libro(1, "50");
        libro.setTitulo("Titulo original");
        libro.setAutor(new Autor(1, "Autor original", ""));
        libro.setEditorial(new Editorial(1, "Editorial original", ""));
        libro.setCategoria(new Categoria(1, "Categoria original", ""));
        Carrito carrito = new Carrito();
        carrito.agregarItem(libro, 1);
        Usuario usuario = new Usuario();
        usuario.setNombre("Comprador original");
        usuario.setEmail("original@bookshop");
        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setCalle("Calle original");
        TarjetaBancaria tarjeta = new TarjetaBancaria();
        tarjeta.setProveedor("Proveedor original");
        tarjeta.setNumeroTarjetaCifrado("valor-cifrado");
        Pedido pedido = new Pedido(1, usuario, carrito, direccion);
        pedido.setMetodoPago(tarjeta);
        pedido.setEstado(EstadoPedido.EN_ALMACEN);

        libro.setTitulo("Otro titulo");
        libro.getAutor().setNombre("Otro autor");
        direccion.setCalle("Otra calle");
        usuario.setNombre("Otro comprador");
        tarjeta.setProveedor("Otro proveedor");

        assertEquals("Otro titulo", pedido.getItems().getFirst().getLibro().getTitulo());
        assertEquals("Otro autor", pedido.getItems().getFirst().getLibro().getAutor().getNombre());
        assertEquals("Otra calle", pedido.getDireccionEnvio().getCalle());
        assertEquals("Otro comprador", pedido.getUsuario().getNombre());
        assertEquals("Otro proveedor", pedido.getMetodoPago().getProveedor());
        assertEquals(0, new BigDecimal("50").compareTo(pedido.getTotal()));
    }

    @Test
    void devolucionRespetaFechaDeEntregaPlazoEIntentos() {
        java.time.LocalDateTime entrega = java.time.LocalDateTime.of(2026, 10, 5, 12, 0);
        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setFechaEntrega(entrega);
        assertTrue(pedido.puedeSolicitarDevolucion(entrega.plusDays(7), 7, 2));
        assertFalse(pedido.puedeSolicitarDevolucion(entrega.plusDays(7).plusSeconds(1), 7, 2));
        assertFalse(pedido.puedeSolicitarDevolucion(entrega.minusSeconds(1), 7, 2));
        pedido.registrarIntento();
        pedido.registrarIntento();
        assertFalse(pedido.puedeSolicitarDevolucion(entrega, 7, 2));
        pedido.setEstado(EstadoPedido.DEVUELTO);
        assertFalse(pedido.puedeSolicitarDevolucion(entrega, 7, 3));
    }
}
