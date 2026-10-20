package me.Business.catalogo;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DatosReferenciaDTO;
import me.Model.dto.catalogo.DetalleReferenciaDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.RegistroReferenciaDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;

public interface AutorBO {
    int registrar(int idActor, RegistroReferenciaDTO registro) throws NegocioException;
    void modificar(int idActor, int id, DatosReferenciaDTO datos) throws NegocioException;
    void cambiarImagen(int idActor, int id, ImagenDTO imagen) throws NegocioException;
    void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException;
    void eliminar(int idActor, int id) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(int idActor, FiltroReferenciaDTO filtro) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscarOpciones(int idActor, FiltroReferenciaDTO filtro) throws NegocioException;
    DetalleReferenciaDTO obtenerDetalle(int idActor, int id) throws NegocioException;
}

