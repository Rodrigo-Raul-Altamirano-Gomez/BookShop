package me.Business.compra;

import me.Model.entities.usuario.Usuario;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.Autorizacion;
import me.Business.Validaciones;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DAO.venta.CuponDAO;
import me.DAO.venta.CuponDAOImpl;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.CuponDTO;

public class CuponBOImpl implements CuponBO {
    private final CuponDAO cupones;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public CuponBOImpl() {
        this(new CuponDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public CuponBOImpl(CuponDAO cupones, UsuarioDAO usuarios, Clock reloj) {
        this.cupones = cupones;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public ResultadoPaginadoDTO<CuponDTO> listar(int idActor, int pagina, int tamanio) throws NegocioException {
        Usuario usuario = autorizacion.usuarioActivo(idActor);
        Validaciones.pagina(pagina, tamanio);
        try {
            cupones.actualizarVigencias(LocalDateTime.now(reloj), usuario.getEmail());
            return cupones.listarPorUsuario(idActor, LocalDateTime.now(reloj), pagina, tamanio);
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los cupones.", e); }
    }
}

