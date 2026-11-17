package me.Model.dto;

import java.util.List;

// Items de la pagina solicitada; totalResultados cuenta todo el conjunto filtrado.
public record ResultadoPaginadoDTO<T>(List<T> items, long totalResultados, int pagina, int tamanioPagina) {
}
