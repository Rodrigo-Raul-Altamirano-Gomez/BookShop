package me.Business.opinion;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.opinion.ConsultaResenasDTO;
import me.Model.dto.opinion.DatosResenaDTO;
import me.Model.dto.opinion.ResenaDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;

public interface ResenaBO {
    void verificarPuedePublicar(int idActor, int idLibro) throws NegocioException;
    int publicar(int idActor, int idLibro, DatosResenaDTO datos) throws NegocioException;
    DatosResenaDTO obtenerParaEditar(int idActor, int idResena) throws NegocioException;
    void modificar(int idActor, int idResena, DatosResenaDTO datos) throws NegocioException;
    void eliminarPropia(int idActor, int idResena) throws NegocioException;
    void eliminarAdministracion(int idActor, int idResena) throws NegocioException;
    ResultadoPaginadoDTO<ResenaDTO> listarPorLibro(int idActor, int idLibro, ConsultaResenasDTO consulta) throws NegocioException;
    ResultadoPaginadoDTO<ResenaDTO> listarPropias(int idActor, ConsultaResenasDTO consulta) throws NegocioException;
    ResultadoPaginadoDTO<ResenaDTO> listarAdministracion(int idActor, ConsultaResenasDTO consulta) throws NegocioException;
    ValoracionLibroDTO obtenerValoracion(int idActor, int idLibro) throws NegocioException;
}
