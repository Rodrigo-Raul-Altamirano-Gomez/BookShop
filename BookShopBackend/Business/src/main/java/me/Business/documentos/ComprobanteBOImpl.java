package me.Business.documentos;

import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.entities.usuario.Usuario;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.Business.compra.PedidoBO;
import me.Business.compra.PedidoBOImpl;
import me.Business.exceptions.NegocioException;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.compra.ComprobanteCompraDTO;
import me.Model.dto.usuario.TarjetaUsuarioDTO;
import java.sql.SQLException;
import me.Business.exceptions.PersistenciaException;

public class ComprobanteBOImpl implements ComprobanteBO {

    private final PedidoBO pedidoBO;
    private final Clock reloj;

    public ComprobanteBOImpl() {
        this(new PedidoBOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public ComprobanteBOImpl(PedidoBO pedidoBO, UsuarioDAO usuarioDAO, Clock reloj) {
        this.pedidoBO = pedidoBO;
        this.usuarioDAO = usuarioDAO;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public ComprobanteCompraDTO preparar(int idActor, int idPedido) throws NegocioException {
        try {
            Usuario comprador = usuarioDAO.leer(idActor);
            DetallePedidoDTO pedido = pedidoBO.consultar(idActor, idPedido);
            return new ComprobanteCompraDTO(pedido, new TarjetaUsuarioDTO(comprador.getId(),
                comprador.getNombre(), comprador.getEmail(), null), LocalDateTime.now(reloj));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la cuenta.", e);
        }
    }

    private final UsuarioDAO usuarioDAO;
}
