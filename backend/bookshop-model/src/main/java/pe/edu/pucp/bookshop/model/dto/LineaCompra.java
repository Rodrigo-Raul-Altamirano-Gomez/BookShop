package pe.edu.pucp.bookshop.model.dto;

import pe.edu.pucp.bookshop.model.Libro;

// Un libro del carrito o del resumen de compra, con sus datos actuales: precio final × cantidad = subtotal
public record LineaCompra(Libro libro, int cantidad, double subtotal) {
}
