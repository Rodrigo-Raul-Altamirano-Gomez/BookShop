package me.Model.dto;

import java.util.List;

// Items de la pagina solicitada; totalResultados cuenta todo el conjunto filtrado.
public record ResultadoPaginadoDTO<T>(
        List<T> items,
        long totalResultados,
        int pagina,
        int tamanioPagina
) {
    public ResultadoPaginadoDTO {
        if (pagina < 1 || tamanioPagina < 1 || totalResultados < 0) {
            throw new IllegalArgumentException("Pagina y tamanio positivos; total no negativo.");
        }
        items = items == null ? List.of() : List.copyOf(items);
    }

    public long totalPaginas() {
        return totalResultados / tamanioPagina + (totalResultados % tamanioPagina == 0 ? 0 : 1);
    }
}
