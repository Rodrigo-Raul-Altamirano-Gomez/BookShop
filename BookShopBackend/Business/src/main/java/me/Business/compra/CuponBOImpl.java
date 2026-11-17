package me.Business.compra;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.Business.exceptions.PersistenciaException;
import me.DAO.venta.CuponDAO;
import me.DAO.venta.CuponDAOImpl;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.CuponDTO;
import me.Model.dto.compra.EstadoCupon;
import me.Model.entities.compra.Cupon;

public class CuponBOImpl implements CuponBO {

    private final CuponDAO cuponDAO = new CuponDAOImpl();

    @Override
    public ResultadoPaginadoDTO<CuponDTO> listar(int idActor, int pagina, int tamanio) throws NegocioException {
        try {
            if (pagina < 1 || tamanio < 1)
                throw new ValidacionException("pagina", "La pagina y su tamaño deben ser positivos.");
            LocalDateTime ahora = LocalDateTime.now();
            ResultadoPaginadoDTO<Cupon> registros = cuponDAO.listarPorUsuario(idActor, pagina, tamanio);
            List<CuponDTO> cupones = new ArrayList<>();
            for (Cupon cupon : registros.items()) {
                EstadoCupon estado = EstadoCupon.DISPONIBLE;
                if (cupon.isConsumido())
                    estado = EstadoCupon.UTILIZADO;
                else if (!cupon.isActivo() || !cupon.estaVigente(ahora))
                    estado = EstadoCupon.VENCIDO;
                cupones.add(new CuponDTO(cupon.getId(), cupon.getPorcentaje(), cupon.getMontoMinimo(),
                    cupon.getFechaFin(), estado));
            }
            return new ResultadoPaginadoDTO<>(cupones, registros.totalResultados(), pagina, tamanio);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los cupones.", e);
        }
    }

}
