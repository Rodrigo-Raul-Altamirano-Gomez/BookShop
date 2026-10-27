package me.Model.dto.compra;

import java.util.ArrayList;
import java.util.List;
import me.Model.dto.ImagenDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TarjetaPedidoDTOTest {
    @Test
    void conservaHuecosDePortadasGenericasSinExponerLaLista() {
        ImagenDTO portada = new ImagenDTO(new byte[]{1, 2}, "image/png");
        List<ImagenDTO> externas = new ArrayList<>(List.of(new ImagenDTO(null, null), portada));
        TarjetaPedidoDTO tarjeta = new TarjetaPedidoDTO(null, externas);
        externas.clear();

        assertEquals(2, tarjeta.portadas().size());
        assertNull(tarjeta.portadas().getFirst().contenido());
        assertArrayEquals(new byte[]{1, 2}, tarjeta.portadas().getLast().contenido());
        assertThrows(UnsupportedOperationException.class, () -> tarjeta.portadas().clear());
    }
}
