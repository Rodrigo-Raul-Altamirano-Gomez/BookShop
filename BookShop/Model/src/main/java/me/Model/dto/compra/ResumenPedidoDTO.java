package me.Model.dto.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import me.Model.entities.compra.EstadoPedido;

// Cabecera de pedidos confirmados y referencia de solicitudes; nunca representa EN_ESPERA.
// Cantidad de libros cuenta ejemplares; la fecha mostrada es la de pago.
public record ResumenPedidoDTO(
        int id,
        LocalDateTime fechaPago,
        EstadoPedido estado,
        int cantidadLibros,
        BigDecimal total
) {
}
