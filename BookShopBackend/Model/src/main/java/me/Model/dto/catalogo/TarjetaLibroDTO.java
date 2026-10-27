package me.Model.dto.catalogo;

import java.math.BigDecimal;
import me.Model.dto.ImagenDTO;

// Sin descuento: porcentaje cero y precioConDescuento igual a precioOriginal.
public record TarjetaLibroDTO(
        int id,
        String titulo,
        ImagenDTO portada,
        String nombreAutor,
        BigDecimal precioOriginal,
        BigDecimal precioConDescuento,
        BigDecimal porcentajeDescuento,
        boolean activo
) {
}
