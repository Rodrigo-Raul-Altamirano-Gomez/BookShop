package me.Model.dto.catalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Consulta de tarjetas seleccionables; estado se calcula con la vigencia al consultar.
public record DescuentoDTO(
        int id,
        BigDecimal porcentaje,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        EstadoDescuento estado
) {
}
