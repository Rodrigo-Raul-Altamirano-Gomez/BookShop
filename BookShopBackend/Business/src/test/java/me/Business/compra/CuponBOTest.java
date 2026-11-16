package me.Business.compra;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import me.DAO.venta.CuponDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.EstadoCupon;
import me.Model.entities.compra.Cupon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CuponBOTest {
    @Test
    void clasificaCuponesSinModificarLosRegistrosConsultados() throws Exception {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 7, 10, 0);
        Cupon disponible = new Cupon(1, null, BigDecimal.TEN, BigDecimal.ZERO,
            fecha.minusDays(1), fecha);
        Cupon usado = new Cupon(2, null, BigDecimal.TEN, BigDecimal.ZERO,
            fecha.minusDays(1), fecha.plusDays(1));
        usado.setConsumido(true);
        Cupon vencido = new Cupon(3, null, BigDecimal.TEN, BigDecimal.ZERO,
            fecha.minusDays(2), fecha.minusDays(1));
        CuponDAO cupones = mock(CuponDAO.class);
        when(cupones.listarPorUsuario(1, 1, 10)).thenReturn(
            new ResultadoPaginadoDTO<>(List.of(disponible, usado, vencido), 3, 1, 10));
        Clock reloj = Clock.fixed(fecha.atZone(ZoneId.of("America/Lima")).toInstant(),
            ZoneId.of("America/Lima"));
        var resultado = new CuponBOImpl(cupones, reloj).listar(1, 1, 10);
        assertEquals(EstadoCupon.DISPONIBLE, resultado.items().get(0).estado());
        assertEquals(EstadoCupon.UTILIZADO, resultado.items().get(1).estado());
        assertEquals(EstadoCupon.VENCIDO, resultado.items().get(2).estado());
        assertEquals(3, resultado.totalResultados());
        verify(cupones).listarPorUsuario(1, 1, 10);
        verifyNoMoreInteractions(cupones);
    }
}
