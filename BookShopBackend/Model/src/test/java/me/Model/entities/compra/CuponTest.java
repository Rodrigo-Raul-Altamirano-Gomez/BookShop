package me.Model.entities.compra;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CuponTest {
    private final LocalDateTime fecha = LocalDateTime.of(2026, 10, 5, 12, 0);

    @Test
    void minimoInclusivoYEstadoConsumidoImpidenUsarloDosVeces() {
        Cupon cupon = cupon();
        assertFalse(cupon.esAplicable(new BigDecimal("99.99"), fecha));
        assertTrue(cupon.esAplicable(new BigDecimal("100"), fecha));
        cupon.setConsumido(true);
        assertTrue(cupon.isConsumido());
        assertTrue(cupon.isActivo());
        assertFalse(cupon.isEliminado());
        assertFalse(cupon.esAplicable(new BigDecimal("100"), fecha));
    }

    @Test
    void vigenciaYEstadoDeterminanSiPuedeAplicarse() {
        Cupon cupon = cupon();
        assertTrue(cupon.esAplicable(new BigDecimal("100"), fecha.minusDays(1)));
        assertTrue(cupon.esAplicable(new BigDecimal("100"), fecha.plusDays(1)));
        assertFalse(cupon.esAplicable(new BigDecimal("100"), fecha.plusDays(2)));
        cupon.setEliminado(true);
        assertFalse(cupon.esAplicable(new BigDecimal("100"), fecha));
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
