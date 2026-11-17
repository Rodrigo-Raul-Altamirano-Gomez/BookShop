package me.Business.documentos;

import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.entities.usuario.Usuario;
import java.time.LocalDateTime;
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

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final PedidoBO pedidoBO = new PedidoBOImpl();
    private final GeneradorPDF generador = new GeneradorPDF();

    private ComprobanteCompraDTO preparar(int idActor, int idPedido) throws NegocioException {
        try {
            Usuario comprador = usuarioDAO.leer(idActor);
            DetallePedidoDTO pedido = pedidoBO.consultar(idActor, idPedido);
            return new ComprobanteCompraDTO(pedido, new TarjetaUsuarioDTO(comprador.getId(),
                comprador.getNombre(), comprador.getEmail(), null), LocalDateTime.now());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la cuenta.", e);
        }
    }

    @Override
    public byte[] generarPDF(int idActor, int idPedido) throws NegocioException {
        return generador.comprobante(preparar(idActor, idPedido));
    }
}
