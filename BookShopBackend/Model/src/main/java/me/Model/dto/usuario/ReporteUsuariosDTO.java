package me.Model.dto.usuario;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

// Conjunto completo del PDF mensual, sin paginacion web. Cantidad maxima nula significa sin limite.
public record ReporteUsuariosDTO(
        YearMonth mes,
        LocalDateTime fechaGeneracion,
        Integer cantidadMaxima,
        List<ResumenActividadUsuarioDTO> resultados
) {
    public ReporteUsuariosDTO {
        resultados = resultados == null ? List.of() : List.copyOf(resultados);
    }

    public int cantidadResultados() {
        return resultados.size();
    }
}
