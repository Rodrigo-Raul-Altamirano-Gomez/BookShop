package me.Model.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImagenDTOTest {
    @Test void conservaLaUrl() {
        ImagenDTO imagen = new ImagenDTO("https://example.test/portada.png");
        assertEquals("https://example.test/portada.png", imagen.url());
    }
    @Test void admiteAusenciaDeUrl() { assertNull(new ImagenDTO(null).url()); }
}
