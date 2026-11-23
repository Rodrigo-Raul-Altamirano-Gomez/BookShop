package pe.edu.pucp.bookshop.model.pedido;

import java.util.List;

// Lo que envía el front para calcular el resumen (solo items e idCupon) o para pagar (todo).
// preparado es el resumen que vio el cliente: al pagar se compara con los datos actuales (ERS RF15.4).
public record SolicitudCompra(int idUsuario,
                              List<ItemCarrito> items,
                              Integer idDireccion,
                              Integer idMetodoPago,
                              Integer idCupon,
                              ResumenCompra preparado) {
}
