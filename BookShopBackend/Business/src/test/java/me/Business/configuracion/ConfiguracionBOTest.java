package me.Business.configuracion;

import java.math.BigDecimal;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import me.Business.exceptions.*;
import me.Model.dto.ConfiguracionDTO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracionBOTest {

    @TempDir
    Path temporal;

    @Test
    void ausenciaUsaInicialSinCrearArchivo() throws Exception {
        Path archivo = temporal.resolve("negocio.properties");
        var bo = new ConfiguracionBOImpl(archivo);
        assertEquals(ConfiguracionBOImpl.inicial(), bo.obtener(1));
        assertFalse(Files.exists(archivo));
    }

    @Test
    void guardarSobreviveReinicioYSeVeDesdeOtraInstancia() throws Exception {
        Path archivo = temporal.resolve("negocio.properties");
        var primera = new ConfiguracionBOImpl(archivo);
        var segunda = new ConfiguracionBOImpl(archivo);
        ConfiguracionDTO nueva = new ConfiguracionDTO(new BigDecimal("7.50"), 20, 2, 4, new BigDecimal("20.00"), new BigDecimal("80.00"), 15);
        primera.guardar(1, nueva);
        assertEquals(nueva, segunda.obtener(1));
        assertEquals(nueva, new ConfiguracionBOImpl(archivo).obtener(1));
    }

    @Test
    void datoInvalidoConservaArchivoYConfiguracionCompleta() throws Exception {
        Path archivo = temporal.resolve("negocio.properties");
        var bo = new ConfiguracionBOImpl(archivo);
        bo.guardar(1, ConfiguracionBOImpl.inicial());
        String previo = Files.readString(archivo);
        var invalida = new ConfiguracionDTO(new BigDecimal("10"), 30, 0, 5, BigDecimal.TEN, BigDecimal.ZERO, 30);
        assertThrows(ValidacionException.class, () -> bo.guardar(1, invalida));
        assertEquals(previo, Files.readString(archivo));
        assertEquals(ConfiguracionBOImpl.inicial(), bo.obtener(1));
    }

    @Test
    void archivoInvalidoNoSeSobrescribe() throws Exception {
        Path archivo = temporal.resolve("negocio.properties");
        Files.writeString(archivo, "porcentajeEnvio=abc");
        assertThrows(ConfiguracionException.class, () -> new ConfiguracionBOImpl(archivo));
        assertEquals("porcentajeEnvio=abc", Files.readString(archivo));
    }

    @Test
    void fallaDeEscrituraNoModificaConfiguracionAnterior() throws Exception {
        Path bloqueo = temporal.resolve("bloqueo");
        var bo = new ConfiguracionBOImpl(bloqueo.resolve("negocio.properties"));
        Files.writeString(bloqueo, "no-es-directorio");
        assertThrows(ConfiguracionException.class, () -> bo.guardar(1, ConfiguracionBOImpl.inicial()));
        assertEquals("no-es-directorio", Files.readString(bloqueo));
        assertEquals(ConfiguracionBOImpl.inicial(), bo.obtener(1));
    }
}
