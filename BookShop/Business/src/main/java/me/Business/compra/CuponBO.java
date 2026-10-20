package me.Business.compra;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.CuponDTO;

public interface CuponBO {
    ResultadoPaginadoDTO<CuponDTO> listar(int idActor, int pagina, int tamanio) throws NegocioException;
}

