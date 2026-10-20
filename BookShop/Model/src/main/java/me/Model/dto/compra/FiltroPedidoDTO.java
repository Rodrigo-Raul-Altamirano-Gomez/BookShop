package me.Model.dto.compra;

import java.util.List;
import me.Model.entities.compra.EstadoPedido;

// Estados vacios incluye todos los confirmados propios; EN_ESPERA no es un filtro del historial.
public record FiltroPedidoDTO(
        List<EstadoPedido> estados,
        OrdenPedido orden,
        int pagina,
        int tamanioPagina
) {
    public FiltroPedidoDTO {
        estados = estados == null ? List.of() : List.copyOf(estados);
    }

    public FiltroPedidoDTO() {
        this(List.of(), OrdenPedido.FECHA_PAGO_DESCENDENTE, 1, 10);
    }
}
