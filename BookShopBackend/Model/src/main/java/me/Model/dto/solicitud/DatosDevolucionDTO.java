package me.Model.dto.solicitud;

import me.Model.entities.solicitud.MotivoDevolucion;

// Alta y edicion: motivo obligatorio y comentario adicional opcional.
// Pedido al crear y solicitud al editar se identifican en la operacion; no se cambia la referencia.
public record DatosDevolucionDTO(
        MotivoDevolucion motivo,
        String comentario
) {
}
