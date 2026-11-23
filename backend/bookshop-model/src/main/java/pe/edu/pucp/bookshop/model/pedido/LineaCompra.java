package pe.edu.pucp.bookshop.model.pedido;

import pe.edu.pucp.bookshop.model.catalogo.Libro;

// Un libro del carrito o del resumen de compra, con sus datos actuales: precio final × cantidad = subtotal
public record LineaCompra(Libro libro, int cantidad, double subtotal) {
}
