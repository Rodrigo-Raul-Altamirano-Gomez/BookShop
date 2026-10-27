package me.Business.compra;

import me.Business.exceptions.NegocioException;
import me.Model.dto.compra.ResumenCompraDTO;

public interface CarritoBO {
    ResumenCompraDTO consultar(int idActor) throws NegocioException;
    ResumenCompraDTO agregar(int idActor, int idLibro, int cantidad) throws NegocioException;
    ResumenCompraDTO cambiarCantidad(int idActor, int idLibro, int cantidad) throws NegocioException;
    ResumenCompraDTO retirar(int idActor, int idLibro) throws NegocioException;
    ResumenCompraDTO vaciar(int idActor) throws NegocioException;
}

