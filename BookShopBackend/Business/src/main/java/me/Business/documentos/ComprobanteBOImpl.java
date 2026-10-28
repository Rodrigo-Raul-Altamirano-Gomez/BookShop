package me.Business.documentos;

import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.entities.usuario.Usuario;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.Business.compra.PedidoBO;
import me.Business.compra.PedidoBOImpl;
import me.Business.exceptions.NegocioException;
import me.Business.Autorizacion;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.compra.ComprobanteCompraDTO;
import me.Model.dto.usuario.TarjetaUsuarioDTO;

public class ComprobanteBOImpl implements ComprobanteBO {
    private final PedidoBO pedidos;
    private final Autorizacion autorizacion;
    private final Clock reloj;
    public ComprobanteBOImpl() {
        this(new PedidoBOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }
    public ComprobanteBOImpl(PedidoBO pedidos, UsuarioDAO usuarios, Clock reloj) {
        this.pedidos = pedidos;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }
    @Override
    public ComprobanteCompraDTO preparar(int idActor, int idPedido) throws NegocioException {
        Usuario comprador = autorizacion.usuarioActivo(idActor);
        DetallePedidoDTO pedido = pedidos.consultar(idActor, idPedido);
        return new ComprobanteCompraDTO(pedido,
                new TarjetaUsuarioDTO(comprador.getId(), comprador.getNombre(), comprador.getEmail(), null),
                LocalDateTime.now(reloj));
    }
}
