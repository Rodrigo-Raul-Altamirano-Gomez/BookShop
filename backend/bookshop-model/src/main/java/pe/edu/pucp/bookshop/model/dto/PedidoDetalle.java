package pe.edu.pucp.bookshop.model.dto;

import pe.edu.pucp.bookshop.model.Pedido;

import java.time.LocalDate;

// Detalle de un pedido con lo que decide sus acciones (UX-UI §9.3): las cancelaciones que le quedan
// al cliente este mes (RN07) y la situación de la devolución (RN08). devolucionHasta es null si no se entregó.
public record PedidoDetalle(Pedido pedido,
                            int cancelacionesRestantes,
                            LocalDate devolucionHasta,
                            boolean devolucionEnPlazo,
                            boolean devolucionPendiente,
                            int solicitudesRestantes) {
}
