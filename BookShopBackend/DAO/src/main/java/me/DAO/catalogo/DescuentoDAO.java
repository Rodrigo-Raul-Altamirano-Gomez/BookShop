package me.DAO.catalogo;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.DAO.CrudDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.entities.catalogo.Descuento;

public interface DescuentoDAO extends CrudDAO<Descuento> {

    ResultadoPaginadoDTO<Descuento> listarPorLibro(int idLibro, int pagina, int
        tamanioPagina) throws SQLException;

    Descuento obtenerVigente(int idLibro, LocalDateTime fecha) throws SQLException;

    boolean haySuperposicion(int idLibro, int idExcluir, LocalDateTime inicio, LocalDateTime fin) throws
        SQLException;
}
