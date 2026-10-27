package me.Model.entities.compra;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CuponTest {
    private final LocalDateTime fecha = LocalDateTime.of(2026, 10, 5, 12, 0);

    @Test
    void minimoInclusivoYConsumoImpidenUsarloDosVeces() {
        Cupon cupon = cupon();
        assertFalse(cupon.esAplicable(new BigDecimal("99.99"), fecha));
        assertTrue(cupon.esAplicable(new BigDecimal("100"), fecha));
        assertEquals(0, new BigDecimal("10").compareTo(cupon.calcularDescuento(new BigDecimal("100"), fecha)));
        cupon.consumir(new BigDecimal("100"), fecha);
        assertTrue(cupon.isConsumido());
        assertTrue(cupon.isActivo());
        assertFalse(cupon.isEliminado());
        assertFalse(cupon.esAplicable(new BigDecimal("100"), fecha));
        assertThrows(IllegalStateException.class, () -> cupon.consumir(new BigDecimal("100"), fecha));
    }

    @Test
    void liberarSoloHaceDisponibleUnCuponVigenteYNoEliminado() {
        Cupon vigente = cupon();
        vigente.consumir(new BigDecimal("100"), fecha);
        vigente.liberar(fecha);
        assertTrue(vigente.esAplicable(new BigDecimal("100"), fecha));

        Cupon vencido = cupon();
        vencido.consumir(new BigDecimal("100"), fecha);
        vencido.liberar(fecha.plusDays(2));
        assertFalse(vencido.isActivo());
        assertFalse(vencido.esAplicable(new BigDecimal("100"), fecha.plusDays(2)));

        vigente.setEliminado(true);
        vigente.liberar(fecha);
        assertFalse(vigente.isActivo());
    }

    private Cupon cupon() {
        Cupon cupon = new Cupon();
        cupon.setPorcentaje(new BigDecimal("10"));
        cupon.setMontoMinimo(new BigDecimal("100"));
        cupon.setFechaInicio(fecha.minusDays(1));
        cupon.setFechaFin(fecha.plusDays(1));
        return cupon;
    }
}
