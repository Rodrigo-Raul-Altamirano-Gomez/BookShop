package me.Model.entities.opinion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResenaTest {
    @Test
    void setPuntuacionAsignaElValorRecibido() {
        Resena resena = new Resena();
        resena.setPuntuacion(4);
        assertEquals(4, resena.getPuntuacion());
        resena.setPuntuacion(7);
        assertEquals(7, resena.getPuntuacion());
    }
}
