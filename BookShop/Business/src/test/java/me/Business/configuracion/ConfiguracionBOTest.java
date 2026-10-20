package me.Business.configuracion;

import java.math.BigDecimal;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import me.Business.exceptions.*;
import me.Business.Autorizacion;
import me.Model.dto.ConfiguracionDTO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracionBOTest {
    @TempDir Path temporal;
    Autorizacion autorizacion = mock(Autorizacion.class);
    @Test void ausenciaUsaInicialSinCrearArchivo() throws Exception {
        Path archivo = temporal.resolve("negocio.properties");
        var bo = new ConfiguracionBOImpl(archivo, autorizacion);
        assertEquals(ConfiguracionBOImpl.inicial(), bo.obtener(1));
        assertFalse(Files.exists(archivo));
    }
    @Test void guardarSobreviveReinicioYSeVeDesdeOtraInstancia() throws Exception {
        Path archivo = temporal.resolve("negocio.properties");
        var primera = new ConfiguracionBOImpl(archivo, autorizacion);
        var segunda = new ConfiguracionBOImpl(archivo, autorizacion);
        ConfiguracionDTO nueva = new ConfiguracionDTO(new BigDecimal("7.50"), 20, 2, 4,
                new BigDecimal("20.00"), new BigDecimal("80.00"), 15);
        primera.guardar(1, nueva);
        assertEquals(nueva, segunda.obtener(1));
        assertEquals(nueva, new ConfiguracionBOImpl(archivo, autorizacion).obtener(1));
    }
    @Test void datoInvalidoConservaArchivoYConfiguracionCompleta() throws Exception {
        Path archivo = temporal.resolve("negocio.properties");
        var bo = new ConfiguracionBOImpl(archivo, autorizacion);
        bo.guardar(1, ConfiguracionBOImpl.inicial());
        String previo = Files.readString(archivo);
        var invalida = new ConfiguracionDTO(new BigDecimal("10"), 30, 0, 5,
                BigDecimal.TEN, BigDecimal.ZERO, 30);
        assertThrows(ValidacionException.class, () -> bo.guardar(1, invalida));
        assertEquals(previo, Files.readString(archivo));
        assertEquals(ConfiguracionBOImpl.inicial(), bo.obtener(1));
    }
    @Test void archivoInvalidoNoSeSobrescribe() throws Exception {
        Path archivo = temporal.resolve("negocio.properties");
        Files.writeString(archivo, "porcentajeEnvio=abc");
        assertThrows(ConfiguracionException.class, () -> new ConfiguracionBOImpl(archivo, autorizacion));
        assertEquals("porcentajeEnvio=abc", Files.readString(archivo));
    }
    @Test void fallaDeEscrituraNoModificaConfiguracionAnterior() throws Exception {
        Path bloqueo = temporal.resolve("bloqueo");
        var bo = new ConfiguracionBOImpl(bloqueo.resolve("negocio.properties"), autorizacion);
        Files.writeString(bloqueo, "no-es-directorio");
        assertThrows(ConfiguracionException.class, () -> bo.guardar(1, ConfiguracionBOImpl.inicial()));
        assertEquals("no-es-directorio", Files.readString(bloqueo));
        assertEquals(ConfiguracionBOImpl.inicial(), bo.obtener(1));
    }
    @Test void consultaYGuardadoRequierenAdministrador() throws Exception {
        doThrow(new AccesoDenegadoException("Solo administrador")).when(autorizacion).administrador(2);
        var bo = new ConfiguracionBOImpl(temporal.resolve("negocio.properties"), autorizacion);
        assertThrows(AccesoDenegadoException.class, () -> bo.obtener(2));
        assertThrows(AccesoDenegadoException.class, () -> bo.guardar(2, ConfiguracionBOImpl.inicial()));
        assertFalse(Files.exists(temporal.resolve("negocio.properties")));
    }
}
