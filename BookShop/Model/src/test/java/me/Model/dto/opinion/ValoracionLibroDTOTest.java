package me.Model.dto.opinion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValoracionLibroDTOTest {
    @Test
    void obtieneCantidadSumaYPromedioDeLaDistribucionCompleta() {
        ValoracionLibroDTO valoracion = new ValoracionLibroDTO(1, 0, 1, 1, 0, 2);
        assertEquals(4, valoracion.cantidadResenas());
        assertEquals(15, valoracion.sumaPuntuaciones());
        assertEquals(3.75, valoracion.promedio());
        assertEquals(2L, valoracion.cantidadCincoEstrellas());
    }

    @Test
    void libroSinResenasTieneRecuentosCeroYPromedioAusente() {
        ValoracionLibroDTO valoracion = new ValoracionLibroDTO(1, 0, 0, 0, 0, 0);
        assertEquals(0L, valoracion.cantidadResenas());
        assertEquals(0L, valoracion.sumaPuntuaciones());
        assertNull(valoracion.promedio());
        assertEquals(0L, valoracion.cantidadUnaEstrella());
        assertEquals(0L, valoracion.cantidadCincoEstrellas());
    }

}
