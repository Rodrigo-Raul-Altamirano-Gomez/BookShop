package me.Model.dto.compra;

import java.util.List;

// Una URL por libro; null indica que se muestra la portada genérica.
// El frontend compone las imagenes y muestra el indicador de las que no caben.
public record TarjetaPedidoDTO(
        ResumenPedidoDTO pedido,
        List<String> portadas
) {
    public TarjetaPedidoDTO {
        if (portadas == null) portadas = List.of();
    }
}
