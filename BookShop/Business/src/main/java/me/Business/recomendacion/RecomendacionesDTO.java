package me.Business.recomendacion;

import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;

// El identificador permite volver a ordenar/paginar el mismo conjunto calculado.
public record RecomendacionesDTO(String idConsulta, ResultadoPaginadoDTO<TarjetaLibroDTO> resultados) { }
