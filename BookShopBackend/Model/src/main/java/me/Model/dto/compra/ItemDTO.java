package me.Model.dto.compra;

import java.math.BigDecimal;
import me.Model.dto.ImagenDTO;

// Identidad e imagen actuales del libro; precios y porcentaje cobrados en pedidos.
// Activo y stock son actuales en carrito/preparacion; nulos en consultas historicas.
public record ItemDTO(
        int idLibro,
        String titulo,
        ImagenDTO portada,
        int cantidad,
        BigDecimal precioOriginal,
        BigDecimal precioUnitario,
        BigDecimal porcentajeDescuento,
        BigDecimal subtotal,
        Boolean activo,
        Integer stockDisponible
) {
    public BigDecimal descuentoTotal() {
        return precioOriginal.subtract(precioUnitario).multiply(BigDecimal.valueOf(cantidad));
    }
}
