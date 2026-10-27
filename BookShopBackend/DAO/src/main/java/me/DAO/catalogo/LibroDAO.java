package me.DAO.catalogo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import me.DAO.CrudDAO;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.dto.catalogo.DetalleLibroDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.GestionLibroDTO;
import me.Model.dto.catalogo.ResumenVentasLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.entities.catalogo.Libro;

public interface LibroDAO extends CrudDAO<Libro> {
    Libro leerParaActualizar(int id) throws SQLException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscar(FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorAutor(int idAutor, FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorCategoria(int idCategoria, FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorEditorial(int idEditorial, FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException;
    ResultadoPaginadoDTO<TarjetaLibroDTO> buscarGuardados(int idUsuario, boolean favorito, FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException;
    DetalleLibroDTO obtenerDetalle(int idLibro, int idUsuario, LocalDateTime fecha) throws SQLException;
    TarjetaLibroDTO obtenerTarjeta(int idLibro, LocalDateTime fecha) throws SQLException;
    GestionLibroDTO obtenerGestion(int idLibro) throws SQLException;
    boolean existeIsbn(String isbn, int idExcluir) throws SQLException;
    ResultadoPaginadoDTO<String> buscarIdiomas(String nombre, boolean soloActivos, int pagina, int tamanioPagina) throws SQLException;
    List<Libro> listarParaRecomendaciones() throws SQLException;
    List<TarjetaLibroDTO> listarTarjetasPorIds(List<Integer> ids, LocalDateTime fecha) throws SQLException;
    List<TarjetaLibroDTO> listarTarjetasSinImagenPorIds(List<Integer> ids, LocalDateTime fecha) throws SQLException;
    boolean cambiarImagen(int idLibro, ImagenDTO imagen, String usuario) throws SQLException;
    boolean cambiarVetoDirecto(int idLibro, boolean vetado, String usuario) throws SQLException;
    boolean descontarStock(int idLibro, int cantidad, String usuario) throws SQLException;
    boolean reponerStock(int idLibro, int cantidad, String usuario) throws SQLException;
    boolean cambiarPrecio(int idLibro, BigDecimal precio, String usuario) throws SQLException;
    boolean cambiarStock(int idLibro, int stock, String usuario) throws SQLException;
    List<ResumenVentasLibroDTO> listarVentasLibros(ConsultaReporteLibrosDTO consulta) throws SQLException;
    void recalcularVetosPorAutor(int idAutor, String usuario) throws SQLException;
    void recalcularVetosPorCategoria(int idCategoria, String usuario) throws SQLException;
    void recalcularVetosPorEditorial(int idEditorial, String usuario) throws SQLException;
}
