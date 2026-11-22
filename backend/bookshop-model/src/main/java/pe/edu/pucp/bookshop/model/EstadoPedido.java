package pe.edu.pucp.bookshop.model;

import java.util.List;

public enum EstadoPedido {
    EN_ALMACEN, EN_CAMINO, ENTREGADO, CANCELADO, DEVUELTO;

    // ERS §2.3: venta válida = unidades de un pedido que no está CANCELADO ni DEVUELTO.
    // Las usan las recomendaciones y los reportes (RN11).
    public static final List<EstadoPedido> VENTA_VALIDA = List.of(EN_ALMACEN, EN_CAMINO, ENTREGADO);
}
