package me.Model.dto.compra;

import java.time.LocalDateTime;
import me.Model.dto.usuario.TarjetaUsuarioDTO;

// PDF bajo demanda: reutiliza el detalle con importes historicos; la foto del comprador no se carga.
public record ComprobanteCompraDTO(
        DetallePedidoDTO pedido,
        TarjetaUsuarioDTO comprador,
        LocalDateTime fechaGeneracion
) {
}
