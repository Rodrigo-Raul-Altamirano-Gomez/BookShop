package me.Business;

import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import me.Business.exceptions.*;
import me.Business.Imagenes;
import me.Model.dto.ImagenDTO;
import static org.junit.jupiter.api.Assertions.*;

class ProteccionDatosTest {
    @TempDir Path temporal;
    String anterior;
    @BeforeEach void preparar() {
        anterior = System.getProperty("bookshop.clave.file");
        System.setProperty("bookshop.clave.file", temporal.resolve("clave-datos.key").toString());
    }
    @AfterEach void restaurar() {
        if (anterior == null) System.clearProperty("bookshop.clave.file");
        else System.setProperty("bookshop.clave.file", anterior);
    }
    @Test void hashUsaSalesDiferentesYVerificaSinCambiarMayusculas() throws Exception {
        String primero = ProteccionDatos.hash("ClaveSegura1");
        String segundo = ProteccionDatos.hash("ClaveSegura1");
        assertNotEquals(primero, segundo);
        assertTrue(ProteccionDatos.verificar("ClaveSegura1", primero));
        assertFalse(ProteccionDatos.verificar("clavesegura1", primero));
        assertFalse(primero.contains("ClaveSegura1"));
    }
    @Test void hashDesconocidoMalformadoOExcesivoNoAutentica() throws Exception {
        assertFalse(ProteccionDatos.verificar("ClaveSegura1", "SHA256$datos-antiguos"));
        assertFalse(ProteccionDatos.verificar("ClaveSegura1", "PBKDF2$999999999$aaaa$bbbb"));
        assertFalse(ProteccionDatos.verificar("ClaveSegura1", "PBKDF2$120000$!$!"));
    }
    @Test void cifradoTieneNonceDistintoYClavePersistente() throws Exception {
        String numero = "1234567890123456";
        String primero = ProteccionDatos.cifrar(numero);
        byte[] clave = Files.readAllBytes(temporal.resolve("clave-datos.key"));
        String segundo = ProteccionDatos.cifrar(numero);
        assertNotEquals(primero, segundo);
        assertFalse(primero.contains(numero));
        assertEquals(numero, ProteccionDatos.descifrar(primero));
        assertEquals(numero, ProteccionDatos.descifrar(segundo));
        assertArrayEquals(clave, Files.readAllBytes(temporal.resolve("clave-datos.key")));
        assertEquals(32, clave.length);
        assertEquals("**** 3456", ProteccionDatos.enmascarar(numero));
    }
    @Test void cifradoModificadoFallaEnLugarDeProducirOtroNumero() throws Exception {
        String cifrado = ProteccionDatos.cifrar("987654321");
        byte[] datos = java.util.Base64.getDecoder().decode(cifrado.substring(7));
        datos[datos.length - 1] ^= 1;
        String alterado = "AESGCM$" + java.util.Base64.getEncoder().encodeToString(datos);
        assertThrows(ProteccionDatosException.class, () -> ProteccionDatos.descifrar(alterado));
    }
    @Test void claveInvalidaSeInformaSinReemplazarla() throws Exception {
        Path archivo = temporal.resolve("clave-datos.key");
        Files.writeString(archivo, "clave-invalida");
        assertThrows(ConfiguracionException.class, () -> ProteccionDatos.cifrar("987654321"));
        assertEquals("clave-invalida", Files.readString(archivo));
    }
    @Test void imagenExigeUnaUrlValida() throws Exception {
        assertDoesNotThrow(() -> Imagenes.validar(new ImagenDTO("https://example.test/portada.jpg")));
        assertDoesNotThrow(() -> Imagenes.validar(new ImagenDTO("/images/portada.png")));
        assertThrows(ValidacionException.class, () -> Imagenes.validar(new ImagenDTO("data:image/png;base64,AAAA")));
        assertThrows(ValidacionException.class, () -> Imagenes.validar(new ImagenDTO("javascript:alert(1)")));
        assertThrows(ValidacionException.class, () -> Imagenes.validar(new ImagenDTO("https://example.test/" + "a".repeat(2048))));
        assertDoesNotThrow(() -> Imagenes.validar(null));
    }
}
