package me.Model.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultadoPaginadoDTOTest {
    @Test
    void cuentaLaUltimaPaginaParcialYElResultadoVacio() {
        assertEquals(3, new ResultadoPaginadoDTO<>(List.of(), 41, 1, 20).totalPaginas());
        assertEquals(2, new ResultadoPaginadoDTO<>(List.of(), 40, 1, 20).totalPaginas());
        assertEquals(0, new ResultadoPaginadoDTO<>(List.of(), 0, 1, 20).totalPaginas());
    }

    @Test
    void noPermiteModificarLosResultadosDesdeUnaListaExterna() {
        List<String> externos = new ArrayList<>(List.of("Libro"));
        ResultadoPaginadoDTO<String> resultado = new ResultadoPaginadoDTO<>(externos, 1, 1, 20);
        externos.clear();
        assertEquals(List.of("Libro"), resultado.items());
        assertThrows(UnsupportedOperationException.class, () -> resultado.items().clear());
    }

    @Test
    void rechazaMetadatosQueImpidanLaPaginacion() {
        assertThrows(IllegalArgumentException.class, () -> new ResultadoPaginadoDTO<>(List.of(), 0, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> new ResultadoPaginadoDTO<>(List.of(), 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResultadoPaginadoDTO<>(List.of(), -1, 1, 20));
    }
}
