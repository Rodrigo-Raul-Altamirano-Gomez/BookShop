package me.Model.dto.compra;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TarjetaPedidoDTOTest {
    @Test
    void conservaPortadasGenericasYUrls() {
        String portada = "https://example.test/imagen.png";
        List<String> externas = new ArrayList<>();
        externas.add(null);
        externas.add(portada);
        TarjetaPedidoDTO tarjeta = new TarjetaPedidoDTO(null, externas);

        assertEquals(2, tarjeta.portadas().size());
        assertNull(tarjeta.portadas().getFirst());
        assertEquals("https://example.test/imagen.png", tarjeta.portadas().getLast());
    }
}
