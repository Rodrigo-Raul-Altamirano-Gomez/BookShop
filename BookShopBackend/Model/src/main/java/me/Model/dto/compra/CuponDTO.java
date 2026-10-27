package me.Model.dto.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tarjeta de consulta; la aplicabilidad a una compra se valida al intentar seleccionarlo.
public record CuponDTO(
        int id,
        BigDecimal porcentaje,
        BigDecimal montoMinimo,
        LocalDateTime fechaFin,
        EstadoCupon estado
) {
}
