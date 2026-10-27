package me.Business;

import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import me.Business.exceptions.*;
import me.Business.Validaciones;
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
    @Test void imagenExigeContenidoRealMimeYLimite() throws Exception {
        ByteArrayOutputStream datos = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", datos);
        assertDoesNotThrow(() -> Validaciones.imagen(new ImagenDTO(datos.toByteArray(), "image/png")));
        assertThrows(ValidacionException.class, () -> Validaciones.imagen(new ImagenDTO(datos.toByteArray(), "image/jpeg")));
        assertThrows(ValidacionException.class, () -> Validaciones.imagen(new ImagenDTO(new byte[]{1,2,3}, "image/png")));
        assertThrows(ValidacionException.class, () -> Validaciones.imagen(new ImagenDTO(new byte[5*1024*1024+1], "image/png")));
        assertDoesNotThrow(() -> Validaciones.imagen(null));
    }
}
