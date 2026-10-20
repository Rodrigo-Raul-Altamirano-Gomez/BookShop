package me.Model.dto.opinion;

// Publicar y editar comparten formulario. Libro/resena se identifican en la operacion; actor en sesion.
public record DatosResenaDTO(
        int puntuacion,
        String comentario
) {
}
