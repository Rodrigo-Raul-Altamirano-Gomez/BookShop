package me.Business.documentos;

import me.Business.exceptions.NegocioException;
import me.Model.dto.compra.ComprobanteCompraDTO;

public interface ComprobanteBO {
    ComprobanteCompraDTO preparar(int idActor, int idPedido) throws NegocioException;
}
