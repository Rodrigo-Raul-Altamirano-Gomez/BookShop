package me.Model.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImagenDTOTest {
    @Test
    void noPermiteCambiarLaImagenMedianteElArrayRecibidoODevuelto() {
        byte[] archivo = {1, 2, 3};
        ImagenDTO imagen = new ImagenDTO(archivo, "image/png");
        archivo[0] = 9;
        byte[] consultado = imagen.contenido();
        consultado[1] = 9;

        assertArrayEquals(new byte[]{1, 2, 3}, imagen.contenido());
    }

    @Test
    void admiteAusenciaDeContenido() {
        assertNull(new ImagenDTO(null, null).contenido());
    }
}
