package me.Business.catalogo;

import java.math.BigDecimal;
import java.util.List;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DatosLibroDTO;
import me.Model.dto.catalogo.DetalleLibroDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.GestionLibroDTO;
import me.Model.dto.catalogo.RegistroLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;

public interface LibroBO {
    int registrar(int idActor, RegistroLibroDTO registro) throws NegocioException;
    void modificar(int idActor, int idLibro, DatosLibroDTO datos) throws NegocioException;
    void cambiarPrecio(int idActor, int idLibro, BigDecimal precio) throws NegocioException;
    void cambiarStock(int idActor, int idLibro, int stock) throws NegocioException;
    void cambiarImagen(int idActor, int idLibro, ImagenDTO imagen) throws NegocioException;
    void cambiarActivo(int idActor, int idLibro, boolean activo) throws NegocioException;
    void eliminar(int idActor, int idLibro) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscar(int idActor, FiltroLibroDTO filtro) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorAutor(int idActor, int idAutor, FiltroLibroDTO filtro) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorCategoria(int idActor, int idCategoria, FiltroLibroDTO filtro) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorEditorial(int idActor, int idEditorial, FiltroLibroDTO filtro) throws NegocioException;
    ResultadoPaginadoDTO<String> buscarIdiomas(int idActor, String nombre, int pagina, int tamanio) throws NegocioException;
    DetalleLibroDTO obtenerDetalle(int idActor, int idLibro) throws NegocioException;
    GestionLibroDTO obtenerGestion(int idActor, int idLibro) throws NegocioException;
    List<DetalleLibroDTO> comparar(int idActor, int primerLibro, int segundoLibro) throws NegocioException;
}
