package me.Model.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultadoPaginadoDTOTest {
    @Test
    void cuentaLaUltimaPaginaParcialYElResultadoVacio() {
        assertEquals(3, new ResultadoPaginadoDTO<>(List.of(), 41, 1, 20).totalPaginas());
        assertEquals(2, new ResultadoPaginadoDTO<>(List.of(), 40, 1, 20).totalPaginas());
        assertEquals(0, new ResultadoPaginadoDTO<>(List.of(), 0, 1, 20).totalPaginas());
    }


}
