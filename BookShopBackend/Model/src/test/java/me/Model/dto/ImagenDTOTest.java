package me.Model.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImagenDTOTest {
    @Test
    void contieneLosDatosDelArchivo() {
        byte[] archivo = {1, 2, 3};
        ImagenDTO imagen = new ImagenDTO(archivo, "image/png");
        assertArrayEquals(new byte[]{1, 2, 3}, imagen.contenido());
    }

    @Test
    void admiteAusenciaDeContenido() {
        assertNull(new ImagenDTO(null, null).contenido());
    }
}
