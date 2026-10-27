package me.Model.dto.usuario;

import java.time.LocalDate;

// Edicion de datos personales. Correo y foto no se modifican desde este formulario.
public record DatosPerfilDTO(
        String nombre,
        LocalDate fechaNacimiento
) {
}
