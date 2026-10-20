package me.Business.compra;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.FiltroPedidoDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.SeleccionCompraDTO;
import me.Model.dto.compra.TarjetaPedidoDTO;

public interface PedidoBO {
    PreparacionCompraDTO preparar(int idActor) throws NegocioException;
    PreparacionCompraDTO consultarPreparacion(int idActor) throws NegocioException;
    PreparacionCompraDTO seleccionar(int idActor, SeleccionCompraDTO seleccion) throws NegocioException;
    DetallePedidoDTO pagar(int idActor) throws NegocioException;
    ResultadoPaginadoDTO<TarjetaPedidoDTO> listar(int idActor, FiltroPedidoDTO filtro) throws NegocioException;
    DetallePedidoDTO consultar(int idActor, int idPedido) throws NegocioException;
    DetallePedidoDTO actualizarSeguimiento(int idActor, int idPedido) throws NegocioException;
    DetallePedidoDTO cancelar(int idActor, int idPedido, String motivo) throws NegocioException;
}

