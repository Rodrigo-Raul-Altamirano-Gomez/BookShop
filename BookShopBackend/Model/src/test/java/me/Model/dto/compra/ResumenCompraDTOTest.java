package me.Model.dto.compra;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenCompraDTOTest {
    @Test
    void conservaElDesgloseSinConsultarPreciosActuales() {
        ItemDTO item = new ItemDTO(1, "Libro", null, 2, new BigDecimal("50.00"),
                new BigDecimal("40.00"), new BigDecimal("20.00"), new BigDecimal("80.00"), null, null);
        List<ItemDTO> items = new ArrayList<>(List.of(item));
        ResumenCompraDTO resumen = new ResumenCompraDTO(items,
                new BigDecimal("100.00"), new BigDecimal("20.00"), 7,
                new BigDecimal("10.00"), new BigDecimal("8.00"), new BigDecimal("5.00"),
                new BigDecimal("4.00"), new BigDecimal("76.00"), 2);
        items.clear();

        assertEquals(1, resumen.items().size());
        assertEquals(2, resumen.cantidadLibros());
        assertEquals(0, new BigDecimal("20.00").compareTo(item.descuentoTotal()));
        assertEquals(0, new BigDecimal("80.00").compareTo(resumen.subtotal()));
        BigDecimal total = resumen.subtotal().subtract(resumen.descuentoCupon()).add(resumen.costoEnvio());
        assertEquals(0, total.compareTo(resumen.total()));
    }
}
