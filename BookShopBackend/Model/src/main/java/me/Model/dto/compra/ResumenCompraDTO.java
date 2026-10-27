package me.Model.dto.compra;

import java.math.BigDecimal;
import java.util.List;

// Carrito sin cupon (id cero e importes de cupon cero), preparacion y desglose historico del pedido.
// Cantidad de ejemplares, no de lineas. Los importes recibidos se calculan o recuperan en negocio/datos.
public record ResumenCompraDTO(
        List<ItemDTO> items,
        BigDecimal subtotalProductos,
        BigDecimal descuentoLibros,
        int idCupon,
        BigDecimal porcentajeCupon,
        BigDecimal descuentoCupon,
        BigDecimal porcentajeEnvio,
        BigDecimal costoEnvio,
        BigDecimal total,
        int cantidadLibros
) {
    public ResumenCompraDTO {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public BigDecimal subtotal() {
        return subtotalProductos.subtract(descuentoLibros);
    }
}
