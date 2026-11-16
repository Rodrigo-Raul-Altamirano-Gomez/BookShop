package me.Business.documentos;

import me.Business.exceptions.NegocioException;
import me.Model.dto.compra.ComprobanteCompraDTO;

public interface ComprobanteBO {

    ComprobanteCompraDTO preparar(int idActor, int idPedido) throws NegocioException;
    // RF17: comprobante en PDF de un pedido propio.
    byte[] generarPDF(int idActor, int idPedido) throws NegocioException;
}
