package me.Business.usuario;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DatosMetodoPagoDTO;
import me.Model.dto.compra.MetodoPagoDTO;

public interface MetodoPagoBO {
    MetodoPagoDTO registrar(int idActor, DatosMetodoPagoDTO datos) throws NegocioException;
    MetodoPagoDTO modificar(int idActor, int idMetodo, DatosMetodoPagoDTO datos) throws NegocioException;
    MetodoPagoDTO consultar(int idActor, int idMetodo) throws NegocioException;
    ResultadoPaginadoDTO<MetodoPagoDTO> listar(int idActor, int pagina, int tamanioPagina) throws NegocioException;
    void eliminar(int idActor, int idMetodo) throws NegocioException;
    void establecerPredeterminado(int idActor, int idMetodo) throws NegocioException;
}
