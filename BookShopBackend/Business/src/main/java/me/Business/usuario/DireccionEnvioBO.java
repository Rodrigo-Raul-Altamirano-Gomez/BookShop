package me.Business.usuario;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DatosDireccionEnvioDTO;
import me.Model.dto.compra.DireccionEnvioDTO;

public interface DireccionEnvioBO {
    DireccionEnvioDTO registrar(int idActor, DatosDireccionEnvioDTO datos) throws NegocioException;
    DireccionEnvioDTO modificar(int idActor, int idDireccion, DatosDireccionEnvioDTO datos) throws NegocioException;
    DireccionEnvioDTO consultar(int idActor, int idDireccion) throws NegocioException;
    ResultadoPaginadoDTO<DireccionEnvioDTO> listar(int idActor, int pagina, int tamanioPagina) throws NegocioException;
    void eliminar(int idActor, int idDireccion) throws NegocioException;
    void establecerPredeterminado(int idActor, int idDireccion) throws NegocioException;
}
