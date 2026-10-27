package me.Business.usuario;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.usuario.ConsultaSeguimientosDTO;
import me.Model.dto.usuario.TipoSeguimiento;

public interface SeguimientoBO {
    void guardar(int idActor, TipoSeguimiento tipo, int idReferencia) throws NegocioException;
    boolean retirar(int idActor, TipoSeguimiento tipo, int idReferencia) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaReferenciaDTO> listar(int idActor, ConsultaSeguimientosDTO consulta)
            throws NegocioException;
}
