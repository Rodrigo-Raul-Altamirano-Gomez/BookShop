package me.Model.dto.catalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Id cero al añadir. Eliminar es la casilla pendiente de Guardar cambios, no un estado persistido.
public record DatosDescuentoDTO(
        int id,
        BigDecimal porcentaje,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean eliminar
) {
}
