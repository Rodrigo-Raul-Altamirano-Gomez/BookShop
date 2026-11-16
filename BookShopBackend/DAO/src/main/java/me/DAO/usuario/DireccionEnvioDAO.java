package me.DAO.usuario;

import java.sql.SQLException;
import me.DAO.CrudDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DireccionEnvioDTO;
import me.Model.entities.compra.DireccionEnvio;

public interface DireccionEnvioDAO extends CrudDAO<DireccionEnvio> {

    ResultadoPaginadoDTO<DireccionEnvioDTO> listarPorUsuario(int idUsuario, int pagina, int tamanioPagina)
        throws SQLException;

    boolean establecerPredeterminado(int idUsuario, int idDireccion, String usuario) throws SQLException;

    boolean quitarPredeterminado(int idDireccion, String usuario) throws SQLException;
}
