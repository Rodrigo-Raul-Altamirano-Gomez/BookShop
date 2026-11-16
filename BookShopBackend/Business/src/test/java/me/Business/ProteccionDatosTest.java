package me.Business;

import org.junit.jupiter.api.*;
import me.Business.exceptions.*;
import static org.junit.jupiter.api.Assertions.*;

class ProteccionDatosTest {

    @Test
    void hashUsaSalesDiferentesYVerificaSinCambiarMayusculas() throws Exception {
        String primero = ProteccionDatos.hash("ClaveSegura1");
        String segundo = ProteccionDatos.hash("ClaveSegura1");
        assertNotEquals(primero, segundo);
        assertTrue(ProteccionDatos.verificar("ClaveSegura1", primero));
        assertFalse(ProteccionDatos.verificar("clavesegura1", primero));
        assertFalse(primero.contains("ClaveSegura1"));
    }

    @Test
    void hashDesconocidoMalformadoOExcesivoNoAutentica() throws Exception {
        assertFalse(ProteccionDatos.verificar("ClaveSegura1", "SHA256$datos-antiguos"));
        assertFalse(ProteccionDatos.verificar("ClaveSegura1", "PBKDF2$999999999$aaaa$bbbb"));
        assertFalse(ProteccionDatos.verificar("ClaveSegura1", "PBKDF2$120000$!$!"));
    }

    @Test
    void cifradoTieneNonceDistintoYSePuedeDescifrar() throws Exception {
        String numero = "1234567890123456";
        String primero = ProteccionDatos.cifrar(numero);
        String segundo = ProteccionDatos.cifrar(numero);
        assertNotEquals(primero, segundo);
        assertFalse(primero.contains(numero));
        assertEquals(numero, ProteccionDatos.descifrar(primero));
        assertEquals(numero, ProteccionDatos.descifrar(segundo));
        assertEquals("**** 3456", ProteccionDatos.enmascarar(numero));
    }

    @Test
    void cifradoModificadoFallaEnLugarDeProducirOtroNumero() throws Exception {
        String cifrado = ProteccionDatos.cifrar("987654321");
        byte[] datos = java.util.Base64.getDecoder().decode(cifrado.substring(7));
        datos[datos.length - 1] ^= 1;
        String alterado = "AESGCM$" + java.util.Base64.getEncoder().encodeToString(datos);
        assertThrows(ProteccionDatosException.class, () -> ProteccionDatos.descifrar(alterado));
    }

}
