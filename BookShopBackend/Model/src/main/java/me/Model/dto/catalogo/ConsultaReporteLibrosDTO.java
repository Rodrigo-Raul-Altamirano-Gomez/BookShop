package me.Model.dto.catalogo;

import java.time.LocalDate;

// Formulario del reporte de libros. Periodo inclusivo; cantidad maxima nula significa sin limite.
public record ConsultaReporteLibrosDTO(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer cantidadMaxima
) {
}
