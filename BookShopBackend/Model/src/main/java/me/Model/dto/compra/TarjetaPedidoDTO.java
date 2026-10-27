package me.Model.dto.compra;

import java.util.List;
import me.Model.dto.ImagenDTO;

// Solo historial: una portada por libro distinto; ImagenDTO sin contenido conserva el lugar de la generica.
// El frontend compone las imagenes y muestra el indicador de las que no caben.
public record TarjetaPedidoDTO(
        ResumenPedidoDTO pedido,
        List<ImagenDTO> portadas
) {
    public TarjetaPedidoDTO {
        portadas = portadas == null ? List.of() : List.copyOf(portadas);
    }
}
