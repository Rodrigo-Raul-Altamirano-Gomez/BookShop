package me.Model.dto.compra;

import java.util.ArrayList;
import java.util.List;
import me.Model.dto.ImagenDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TarjetaPedidoDTOTest {
    @Test
    void conservaHuecosDePortadasGenericasSinExponerLaLista() {
        ImagenDTO portada = new ImagenDTO("https://example.test/imagen.png");
        List<ImagenDTO> externas = new ArrayList<>(List.of(new ImagenDTO(null), portada));
        TarjetaPedidoDTO tarjeta = new TarjetaPedidoDTO(null, externas);
        externas.clear();

        assertEquals(2, tarjeta.portadas().size());
        assertNull(tarjeta.portadas().getFirst().url());
        assertEquals("https://example.test/imagen.png", tarjeta.portadas().getLast().url());
        assertThrows(UnsupportedOperationException.class, () -> tarjeta.portadas().clear());
    }
}
