package me.Model.dto.solicitud;

import java.util.List;
import me.Model.entities.solicitud.EstadoSolicitud;

// Mis solicitudes y Moderacion comparten filtro; el alcance propio/administrativo lo fija la operacion.
// Estados vacios incluye todos los no retirados. Orden fijo por fecha de solicitud descendente.
// La respuesta usa ResultadoPaginadoDTO<ResumenSolicitudDTO>, con la cantidad de la pestaña consultada.
public record FiltroSolicitudDTO(
        TipoSolicitud tipo,
        List<EstadoSolicitud> estados,
        int pagina,
        int tamanioPagina
) {
    public FiltroSolicitudDTO {
        estados = estados == null ? List.of() : List.copyOf(estados);
    }

    public FiltroSolicitudDTO() {
        this(TipoSolicitud.DEVOLUCION, List.of(), 1, 10);
    }
}
