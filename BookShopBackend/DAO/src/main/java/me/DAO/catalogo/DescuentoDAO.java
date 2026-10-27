package me.DAO.catalogo;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.DAO.CrudDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DescuentoDTO;
import me.Model.entities.catalogo.Descuento;

public interface DescuentoDAO extends CrudDAO<Descuento> {
    Descuento leerParaActualizar(int id) throws SQLException;
    ResultadoPaginadoDTO<DescuentoDTO> listarPorLibro(int idLibro, LocalDateTime fecha, int pagina, int tamanioPagina) throws SQLException;
    Descuento obtenerVigente(int idLibro, LocalDateTime fecha) throws SQLException;
    void actualizarVigencias(LocalDateTime fecha, String usuario) throws SQLException;
    boolean haySuperposicion(int idLibro, int idExcluir, LocalDateTime inicio, LocalDateTime fin) throws SQLException;
}
