package me.DAO.catalogo;

import java.sql.SQLException;
import java.util.List;
import me.DAO.CrudDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.opinion.*;
import me.Model.entities.opinion.Resena;

public interface ResenaDAO extends CrudDAO<Resena> {

    ResultadoPaginadoDTO<ResenaDTO> listar(int idLibro, int idUsuario, int idUsuarioActual, ConsultaResenasDTO
        consulta) throws SQLException;

    boolean existePublicada(int idUsuario, int idLibro) throws SQLException;

    ValoracionLibroDTO obtenerValoracion(int idLibro) throws SQLException;

    List<ValoracionLibroDTO> listarValoraciones() throws SQLException;
}
