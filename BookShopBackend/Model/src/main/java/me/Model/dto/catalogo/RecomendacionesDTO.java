package me.Model.dto.catalogo;

import me.Model.dto.ResultadoPaginadoDTO;

// El identificador permite volver a ordenar/paginar el mismo conjunto calculado.
public record RecomendacionesDTO(String idConsulta, ResultadoPaginadoDTO<TarjetaLibroDTO> resultados) {
}
