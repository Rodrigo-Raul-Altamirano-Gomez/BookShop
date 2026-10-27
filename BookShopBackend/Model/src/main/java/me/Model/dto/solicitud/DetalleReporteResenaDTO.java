package me.Model.dto.solicitud;

import java.time.LocalDateTime;
import me.Model.dto.opinion.DatosResenaDTO;

// Consulta administrativa y precarga de edicion propia; solicitante y estado estan en solicitud.
// Compara contenidoOriginal de resenaReportada con contenidoActual; editar el reporte no cambia el original.
// Resena activa equivale a disponible (activo y no eliminado). Fecha de resolucion nula si esta pendiente.
public record DetalleReporteResenaDTO(
        ResumenSolicitudDTO solicitud,
        DatosReporteResenaDTO datos,
        ResenaReportadaDTO resenaReportada,
        DatosResenaDTO contenidoActual,
        boolean resenaActiva,
        LocalDateTime fechaResolucion
) {
}
