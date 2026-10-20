package me.Model.dto.catalogo;

import me.Model.dto.ImagenDTO;

// Compartida por autor, categoria y editorial. En opciones de busqueda no hace falta cargar la imagen.
public record TarjetaReferenciaDTO(
        int id,
        String nombre,
        ImagenDTO imagen
) {
}
