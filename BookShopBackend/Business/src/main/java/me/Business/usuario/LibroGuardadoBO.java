package me.Business.usuario;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;

public interface LibroGuardadoBO {
    void guardar(int idActor, int idLibro, boolean favorito) throws NegocioException;
    boolean retirar(int idActor, int idLibro, boolean favorito) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> listar(int idActor, boolean favorito, FiltroLibroDTO filtro)
            throws NegocioException;
}
