package me.DAO.catalogo;

import java.sql.SQLException;
import java.time.LocalDate;
import me.DAO.CrudDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DetalleReferenciaDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.entities.catalogo.Editorial;

public interface EditorialDAO extends CrudDAO<Editorial> {

    ResultadoPaginadoDTO<TarjetaReferenciaDTO> buscar(FiltroReferenciaDTO filtro) throws SQLException;

    DetalleReferenciaDTO obtenerDetalle(int id, int idUsuario) throws SQLException;

    boolean cambiarImagen(int id, String imagen, String usuario) throws SQLException;

    boolean cambiarActivo(int id, boolean activo, String usuario) throws SQLException;

    boolean existeNombre(String nombre, int idExcluir) throws SQLException;

    TarjetaReferenciaDTO obtenerMasComprada(int idUsuario, LocalDate inicio, LocalDate fin) throws SQLException;
}
