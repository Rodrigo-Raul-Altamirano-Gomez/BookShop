package me.Model.dto.catalogo;

// Para ti y Mas populares: ordenar el conjunto calculado antes de devolver la pagina.
// No admite filtros de catalogo ni el orden MAS_VENDIDOS; la operacion determina el algoritmo.
public record ConsultaRecomendacionesDTO(
        OrdenLibro orden,
        int pagina,
        int tamanioPagina
) {
    public ConsultaRecomendacionesDTO() {
        this(OrdenLibro.RECOMENDADOS_PRIMERO, 1, 12);
    }
}
