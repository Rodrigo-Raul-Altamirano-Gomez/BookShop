package me.Model.dto.compra;

import java.time.LocalDateTime;

// Datos de compras validas para los algoritmos; no es una entidad ni un reporte persistido.
public record VentaLibroDTO(
        int idPedido,
        int idUsuario,
        int idLibro,
        int cantidad,
        LocalDateTime fechaPago
) {
}
