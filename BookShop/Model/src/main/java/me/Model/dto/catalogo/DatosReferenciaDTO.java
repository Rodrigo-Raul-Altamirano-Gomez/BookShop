package me.Model.dto.catalogo;

// Introduccion representa biografia de autor o descripcion de categoria/editorial.
// Edicion de nombre y texto; el id se recibe en la operacion y la imagen se modifica por separado.
public record DatosReferenciaDTO(
        String nombre,
        String introduccion
) {
}
