package me.DAO.catalogo;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import me.DAO.CrudDAO;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DetalleReferenciaDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.entities.catalogo.Autor;

public interface AutorDAO extends CrudDAO<Autor> {
    Autor leerParaActualizar(int id) throws SQLException;
    ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(FiltroReferenciaDTO filtro) throws SQLException;
    ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscarOpciones(FiltroReferenciaDTO filtro) throws SQLException;
    DetalleReferenciaDTO obtenerDetalle(int id, int idUsuario) throws SQLException;
    boolean cambiarImagen(int id, ImagenDTO imagen, String usuario) throws SQLException;
    boolean cambiarActivo(int id, boolean activo, String usuario) throws SQLException;
    Map<Integer, TarjetaReferenciaDTO> obtenerMasCompradas(List<Integer> idsUsuarios, LocalDate inicio, LocalDate fin) throws SQLException;
}
