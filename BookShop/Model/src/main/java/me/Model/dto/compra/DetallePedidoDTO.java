package me.Model.dto.compra;

import java.time.LocalDateTime;

// Confirmacion, detalle y consulta de referencia reutilizan la misma informacion del pedido.
// Fechas aun no producidas son nulas; cancelados/devueltos se muestran sin datos de seguimiento.
// En consulta sin operaciones, ambas acciones son falsas; comprobante/actualizacion dependen del contexto.
public record DetallePedidoDTO(
        ResumenPedidoDTO pedido,
        ResumenCompraDTO resumen,
        DireccionEnvioDTO direccionEnvio,
        MetodoPagoDTO metodoPago,
        String identificadorTransaccion,
        String codigoSeguimiento,
        LocalDateTime fechaEntregaEstimada,
        LocalDateTime fechaSalida,
        LocalDateTime fechaEntrega,
        boolean puedeCancelar,
        boolean puedeSolicitarDevolucion
) {
}
