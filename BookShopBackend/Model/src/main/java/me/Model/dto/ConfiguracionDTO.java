package me.Model.dto;

import java.math.BigDecimal;

// Solo los siete parametros editables; no aplica ni almacena configuracion por si mismo.
public record ConfiguracionDTO(
        BigDecimal porcentajeEnvio,
        int plazoDiasDevolucion,
        int maximoIntentosDevolucion,
        int pedidosEntregadosPorCupon,
        BigDecimal porcentajeCupon,
        BigDecimal montoMinimoCupon,
        int vigenciaDiasCupon
) {
}
