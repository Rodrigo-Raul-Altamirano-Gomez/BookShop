package me.Business.recomendacion;

import java.util.List;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.ConsultaRecomendacionesDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;

public interface RecomendacionBO {
    List<TarjetaLibroDTO> similares(int idLibro) throws NegocioException;
    List<TarjetaLibroDTO> complementarios(List<Integer> idsCarrito) throws NegocioException;
    boolean puedeMostrarParaTi(int idActor) throws NegocioException;
    RecomendacionesDTO abrirPopulares(ConsultaRecomendacionesDTO consulta) throws NegocioException;
    RecomendacionesDTO abrirParaTi(int idActor, ConsultaRecomendacionesDTO consulta) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> consultar(int idActor, String idConsulta, ConsultaRecomendacionesDTO consulta) throws NegocioException;
    void descartar(int idActor, String idConsulta) throws NegocioException;
}
