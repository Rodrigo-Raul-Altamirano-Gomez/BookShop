package me.Model.dto.catalogo;

import java.math.BigDecimal;

// Sin descuento: porcentaje cero y precioConDescuento igual a precioOriginal.
public record TarjetaLibroDTO(
        int id,
        String titulo,
        String portada,
        String nombreAutor,
        BigDecimal precioOriginal,
        BigDecimal precioConDescuento,
        BigDecimal porcentajeDescuento,
        boolean activo
) {
}
