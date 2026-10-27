package me.Model.dto.solicitud;

import me.Model.entities.solicitud.MotivoReporteResena;

// Alta y edicion: motivo obligatorio y comentario adicional opcional.
// Resena al crear y solicitud al editar se identifican aparte; el servidor conserva el contenido original.
public record DatosReporteResenaDTO(
        MotivoReporteResena motivo,
        String comentario
) {
}
