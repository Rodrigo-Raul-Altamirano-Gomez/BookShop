package me.Model.dto.catalogo;

import me.Model.dto.ResultadoPaginadoDTO;

// El identificador permite filtrar y paginar conservando el conjunto y el orden del algoritmo.
public record RecomendacionesDTO(String idConsulta, ResultadoPaginadoDTO<TarjetaLibroDTO> resultados) {
}
