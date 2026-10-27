package me.Model.entities.compra;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetodoPagoTest {
    @Test
    void tarjetaActivaConVencimientoFuturoEsValida() {
        TarjetaBancaria tarjeta = new TarjetaBancaria();
        tarjeta.setFechaVencimiento(LocalDate.now().plusDays(1));
        assertTrue(tarjeta.validarEstado());
    }

    @Test
    void tarjetaActivaQueVenceHoyEsValida() {
        TarjetaBancaria tarjeta = new TarjetaBancaria();
        tarjeta.setFechaVencimiento(LocalDate.now());
        assertTrue(tarjeta.validarEstado());
    }

    @Test
    void tarjetaVencidaNoEsValida() {
        TarjetaBancaria tarjeta = new TarjetaBancaria();
        tarjeta.setFechaVencimiento(LocalDate.now().minusDays(1));
        assertFalse(tarjeta.validarEstado());
    }

    @Test
    void tarjetaSinFechaNoEsValida() {
        TarjetaBancaria tarjeta = new TarjetaBancaria();
        assertFalse(tarjeta.validarEstado());
    }

    @Test
    void tarjetaInactivaNoEsValida() {
        TarjetaBancaria tarjeta = new TarjetaBancaria();
        tarjeta.setFechaVencimiento(LocalDate.now().plusDays(1));
        tarjeta.setActivo(false);
        assertFalse(tarjeta.validarEstado());
    }

    @Test
    void billeteraActivaConVencimientoFuturoEsValida() {
        BilleteraDigital billetera = new BilleteraDigital();
        billetera.setFechaVencimiento(LocalDate.now().plusDays(1));
        assertTrue(billetera.validarEstado());
    }

    @Test
    void billeteraInactivaNoEsValida() {
        BilleteraDigital billetera = new BilleteraDigital();
        billetera.setFechaVencimiento(LocalDate.now().plusDays(1));
        billetera.setActivo(false);
        assertFalse(billetera.validarEstado());
    }

    @Test
    void billeteraSinVencimientoNoEsValida() {
        BilleteraDigital billetera = new BilleteraDigital();
        assertFalse(billetera.validarEstado());
    }

    @Test
    void ambosMetodosSonValidosHastaSuFechaDeVencimientoInclusive() {
        LocalDate vencimiento = LocalDate.of(2026, 10, 4);
        MetodoPago[] metodos = {new TarjetaBancaria(), new BilleteraDigital()};
        for (MetodoPago metodo : metodos) {
            metodo.setFechaVencimiento(vencimiento);
            assertTrue(metodo.validarEstado(vencimiento));
            assertFalse(metodo.validarEstado(vencimiento.plusDays(1)));
        }
    }
}
