package me.Model.dto.catalogo;

import java.math.BigDecimal;

// Importes historicos de las lineas despues del descuento del libro, antes de cupon y envio.
public record ResumenVentasLibroDTO(
        int idLibro,
        String titulo,
        String categoria,
        String autor,
        String editorial,
        long cantidadVendida,
        BigDecimal importeVentas
) {
}
