package me.Business;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ProteccionDatosException;
import me.Business.exceptions.ValidacionException;

public final class ProteccionDatos {

    private static final SecureRandom AZAR = new SecureRandom();
    private static final int ITERACIONES = 120_000;
    private static final SecretKey CLAVE = new SecretKeySpec(
        Base64.getDecoder().decode("V3ywfN9h31m55T64XGclaQBYOz1C8wxgGiTqGVYdTuc="), "AES");
    private ProteccionDatos() {
    }

    public static String hash(String contrasenia) throws NegocioException {
        if (contrasenia == null)
            throw new ValidacionException("contrasenia", "Falta la contraseña.");
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        try {
            byte[] resultado = derivar(contrasenia, sal, ITERACIONES);
            return "PBKDF2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal) + "$" +
                Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException e) {
            throw new ProteccionDatosException("No se pudo proteger la contraseña.", e);
        }
    }

    public static boolean verificar(String contrasenia, String hash) throws NegocioException {
        if (contrasenia == null || hash == null)
            return false;
        try {
            String[] partes = hash.split("\\$", -1);
            if (partes.length != 4 || !partes[0].equals("PBKDF2"))
                return false;
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < 10_000 || iteraciones > 1_000_000)
                return false;
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (sal.length != 16 || esperado.length != 32)
                return false;
            return MessageDigest.isEqual(esperado, derivar(contrasenia, sal, iteraciones));
        } catch (IllegalArgumentException e) {
            return false;
        } catch (GeneralSecurityException e) {
            throw new ProteccionDatosException("No se pudo verificar la contraseña.", e);
        }
    }

    private static byte[] derivar(String clave, byte[] sal, int iteraciones) throws GeneralSecurityException {
        // PBKDF2 combina la contraseña con una sal distinta y repite el cálculo para dificultar ataques.
        PBEKeySpec datos = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(datos).getEncoded();
        } finally {
            datos.clearPassword();
        }
    }

    public static String cifrar(String numero) throws NegocioException {
        if (numero == null)
            throw new ValidacionException("numero", "Falta el número del método.");
        try {
            // GCM necesita un número aleatorio por cifrado; también detecta datos modificados.
            byte[] nonce = new byte[12];
            AZAR.nextBytes(nonce);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, CLAVE, new GCMParameterSpec(128, nonce));
            byte[] cifrado = cifrador.doFinal(numero.getBytes(StandardCharsets.UTF_8));
            byte[] datos = Arrays.copyOf(nonce, nonce.length + cifrado.length);
            System.arraycopy(cifrado, 0, datos, nonce.length, cifrado.length);
            return "AESGCM$" + Base64.getEncoder().encodeToString(datos);
        } catch (GeneralSecurityException e) {
            throw new ProteccionDatosException("No se pudo proteger el número del método.", e);
        }
    }

    public static String descifrar(String cifrado) throws NegocioException {
        if (cifrado == null || !cifrado.startsWith("AESGCM$"))
            throw new ProteccionDatosException("El método tiene un formato de protección desconocido.");
        try {
            byte[] datos = Base64.getDecoder().decode(cifrado.substring(7));
            if (datos.length < 28)
                throw new IllegalArgumentException("Cifrado incompleto");
            Cipher descifrador = Cipher.getInstance("AES/GCM/NoPadding");
            descifrador.init(Cipher.DECRYPT_MODE, CLAVE, new GCMParameterSpec(128, datos, 0, 12));
            return new String(descifrador.doFinal(datos, 12, datos.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new ProteccionDatosException("No se pudo leer el número protegido del método.", e);
        }
    }

    public static String enmascarar(String numero) throws NegocioException {
        if (numero == null || numero.length() < 4)
            throw new ProteccionDatosException("El número del método no es válido.");
        return "**** " + numero.substring(numero.length() - 4);
    }

}
