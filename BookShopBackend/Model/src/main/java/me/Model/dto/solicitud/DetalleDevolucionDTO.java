package me.Model.dto.solicitud;

import java.time.LocalDateTime;
import me.Model.dto.compra.ResumenPedidoDTO;

// Consulta administrativa y precarga de edicion propia; el nombre del solicitante esta en solicitud.
// Pedido es la referencia resumida de moderacion. Ver pedido personal reutiliza DetallePedidoDTO sin acciones.
// Estado de solicitud indica el resultado; fechaResolucion es nula mientras este pendiente.
public record DetalleDevolucionDTO(
        ResumenSolicitudDTO solicitud,
        DatosDevolucionDTO datos,
        ResumenPedidoDTO pedido,
        LocalDateTime fechaResolucion
) {
}
