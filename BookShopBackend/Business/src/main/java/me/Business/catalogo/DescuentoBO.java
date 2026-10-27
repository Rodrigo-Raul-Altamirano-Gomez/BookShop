package me.Business.catalogo;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DatosDescuentoDTO;
import me.Model.dto.catalogo.DescuentoDTO;

public interface DescuentoBO {
    int guardar(int idActor, int idLibro, DatosDescuentoDTO datos) throws NegocioException;
    ResultadoPaginadoDTO<DescuentoDTO> listarPorLibro(int idActor, int idLibro, int pagina, int tamanio) throws NegocioException;
}
