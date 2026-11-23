package pe.edu.pucp.bookshop.bl.pedido;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;
import pe.edu.pucp.bookshop.model.pedido.EstadoPedido;
import pe.edu.pucp.bookshop.model.pedido.Pedido;
import pe.edu.pucp.bookshop.model.pedido.PedidoDetalle;
import pe.edu.pucp.bookshop.model.pedido.ResumenCompra;
import pe.edu.pucp.bookshop.model.pedido.SolicitudCompra;

public interface PedidoBL {
    ResumenCompra calcularResumen(SolicitudCompra solicitud) throws BusinessLogicException;
    Pedido pagar(SolicitudCompra solicitud) throws BusinessLogicException;
    ResultadoPagina<Pedido> listarPorUsuario(int idUsuario, EstadoPedido estado, String orden, int pagina, int tamano) throws BusinessLogicException;
    PedidoDetalle obtener(int id) throws BusinessLogicException;
    void cancelar(int id) throws BusinessLogicException;
    byte[] generarComprobante(int id) throws BusinessLogicException;
    // Lo llama el hilo del sistema cada minuto (RF03.4)
    void avanzarSeguimiento() throws BusinessLogicException;
}
