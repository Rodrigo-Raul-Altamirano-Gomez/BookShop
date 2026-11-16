package me.DAO.usuario;

import java.sql.SQLException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.OrdenReferencia;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.usuario.TipoSeguimiento;

public interface SeguimientoDAO {

    void guardar(int idUsuario, TipoSeguimiento tipo, int idReferencia, String usuario) throws SQLException;

    boolean retirar(int idUsuario, TipoSeguimiento tipo, int idReferencia, String usuario) throws SQLException;

    ResultadoPaginadoDTO<TarjetaReferenciaDTO> listarPorUsuario(int idUsuario, TipoSeguimiento tipo,
        OrdenReferencia orden, int pagina, int tamanioPagina, String nombre) throws SQLException;
}
