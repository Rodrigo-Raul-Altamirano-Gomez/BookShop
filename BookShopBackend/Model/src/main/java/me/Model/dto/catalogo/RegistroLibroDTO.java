package me.Model.dto.catalogo;

import java.math.BigDecimal;

// Alta completa: referencias iniciales, precio, stock y portada opcional se guardan con los datos.
public record RegistroLibroDTO(
        DatosLibroDTO datos,
        int idAutor,
        int idCategoria,
        int idEditorial,
        BigDecimal precioBase,
        int stockInicial,
        String portada
) {
}
