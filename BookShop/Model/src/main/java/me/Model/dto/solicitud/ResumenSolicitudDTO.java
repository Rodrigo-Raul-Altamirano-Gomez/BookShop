package me.Model.dto.solicitud;

import java.time.LocalDateTime;
import me.Model.entities.solicitud.EstadoSolicitud;

// Tarjeta y cabecera comunes: idReferencia es pedido o resena segun tipo; motivo es su texto descriptivo.
// Nombre del solicitante para moderacion; puede omitirse en el listado propio. No requiere correo/foto.
// Editar/Retirar dependen de PENDIENTE y del contexto propio; resueltas solo se consultan.
public record ResumenSolicitudDTO(
        int id,
        TipoSolicitud tipo,
        LocalDateTime fechaSolicitud,
        EstadoSolicitud estado,
        String nombreSolicitante,
        String motivo,
        int idReferencia
) {
}
