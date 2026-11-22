package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.EstadoPedido;
import pe.edu.pucp.bookshop.model.Pedido;
import pe.edu.pucp.bookshop.model.dto.PedidoDetalle;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;
import pe.edu.pucp.bookshop.model.dto.ResumenCompra;
import pe.edu.pucp.bookshop.model.dto.SolicitudCompra;

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
