package me.Service;

import java.time.LocalDate;
import java.util.stream.Stream;
import jakarta.json.bind.JsonbBuilder;
import me.Model.dto.catalogo.ConsultaRecomendacionesDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.compra.FiltroPedidoDTO;
import me.Model.dto.opinion.ConsultaResenasDTO;
import me.Model.dto.solicitud.FiltroSolicitudDTO;
import me.Model.dto.usuario.ConsultaSeguimientosDTO;
import me.Model.dto.usuario.RegistroUsuarioDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonbDTOTest {

    static Stream<Object> consultas() {
        return Stream.of(FiltroLibroDTO.porDefecto(), FiltroReferenciaDTO.porDefecto(), ConsultaSeguimientosDTO.porDefecto(), ConsultaResenasDTO.porDefecto(), FiltroPedidoDTO.porDefecto(), FiltroSolicitudDTO.porDefecto(), ConsultaRecomendacionesDTO.porDefecto());
    }

    @ParameterizedTest
    @MethodSource("consultas")
    void conservaConsultasAlConvertirConJsonbEstandar(Object consulta) throws Exception {
        try (var jsonb = JsonbBuilder.create()) {
            String json = jsonb.toJson(consulta);
            assertEquals(consulta, jsonb.fromJson(json, consulta.getClass()));
        }
    }

    @Test
    void conservaFechaYUrlEnRegistro() throws Exception {
        var original = new RegistroUsuarioDTO("Ana", "ana@correo.pe", LocalDate.of(2000, 2, 29), "clave123", "clave123", "https://example.test/imagen.png");
        try (var jsonb = JsonbBuilder.create()) {
            var recibido = jsonb.fromJson(jsonb.toJson(original), RegistroUsuarioDTO.class);
            assertEquals(original.fechaNacimiento(), recibido.fechaNacimiento());
            assertEquals(original.email(), recibido.email());
            assertEquals(original.imagen(), recibido.imagen());
        }
    }
}
