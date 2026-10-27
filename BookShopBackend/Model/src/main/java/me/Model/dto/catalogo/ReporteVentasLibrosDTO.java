package me.Model.dto.catalogo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Conjunto completo del PDF, sin paginacion web. Cantidad maxima nula significa sin limite.
public record ReporteVentasLibrosDTO(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalDateTime fechaGeneracion,
        Integer cantidadMaxima,
        List<ResumenVentasLibroDTO> resultados
) {
    public ReporteVentasLibrosDTO {
        resultados = resultados == null ? List.of() : List.copyOf(resultados);
    }

    public int cantidadResultados() {
        return resultados.size();
    }
}
