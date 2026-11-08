package me.Business.compra;

import me.Model.entities.usuario.Usuario;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.Business.exceptions.PersistenciaException;
import me.Business.Autorizacion;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DAO.venta.CuponDAO;
import me.DAO.venta.CuponDAOImpl;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.CuponDTO;

public class CuponBOImpl implements CuponBO {
    private final CuponDAO cuponDAO;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public CuponBOImpl() {
        this(new CuponDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public CuponBOImpl(CuponDAO cuponDAO, UsuarioDAO usuarioDAO, Clock reloj) {
        this.cuponDAO = cuponDAO;
        this.autorizacion = new Autorizacion(usuarioDAO);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public ResultadoPaginadoDTO<CuponDTO> listar(int idActor, int pagina, int tamanio) throws NegocioException {
        Usuario usuario = autorizacion.usuarioActivo(idActor);
        if (pagina < 1 || tamanio < 1)
            throw new ValidacionException("pagina", "La pagina y su tamaño deben ser positivos.");
        try {
            cuponDAO.actualizarVigencias(LocalDateTime.now(reloj), usuario.getEmail());
            return cuponDAO.listarPorUsuario(idActor, LocalDateTime.now(reloj), pagina, tamanio);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los cuponDAO.", e);
        }
    }
}
