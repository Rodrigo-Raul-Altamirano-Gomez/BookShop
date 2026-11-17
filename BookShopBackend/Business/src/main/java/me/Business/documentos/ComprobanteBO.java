package me.Business.documentos;

import me.Business.exceptions.NegocioException;

public interface ComprobanteBO {

    // RF17: comprobante en PDF de un pedido propio.
    byte[] generarPDF(int idActor, int idPedido) throws NegocioException;
}
