package me.Business;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import me.Business.exceptions.NegocioException;

// Protege los datos sensibles:
// - Contraseñas: se guarda un hash PBKDF2 (no se puede revertir), con el formato PBKDF2$iteraciones$sal$hash.
// - Números de tarjeta o celular: se cifran con AES para poder leerlos después, con el formato AESGCM$datos.
public class ProteccionDatos {

    private static final int ITERACIONES = 120000;
    private static final SecretKeySpec CLAVE = new SecretKeySpec(
        Base64.getDecoder().decode("V3ywfN9h31m55T64XGclaQBYOz1C8wxgGiTqGVYdTuc="), "AES");
    private static final SecureRandom azar = new SecureRandom();

    public static String hash(String contrasenia) throws NegocioException {
        byte[] sal = new byte[16];
        azar.nextBytes(sal);
        byte[] resultado = derivar(contrasenia, sal, ITERACIONES);
        return "PBKDF2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal) + "$"
            + Base64.getEncoder().encodeToString(resultado);
    }

    // Repite el hash con la misma sal guardada y compara los resultados.
    public static boolean verificar(String contrasenia, String hashGuardado) throws NegocioException {
        String[] partes = hashGuardado.split("\\$");
        int iteraciones = Integer.parseInt(partes[1]);
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        return Arrays.equals(esperado, derivar(contrasenia, sal, iteraciones));
    }

    private static byte[] derivar(String contrasenia, byte[] sal, int iteraciones) throws NegocioException {
        try {
            PBEKeySpec datos = new PBEKeySpec(contrasenia.toCharArray(), sal, iteraciones, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(datos).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new NegocioException("No se pudo proteger la contraseña.", e);
        }
    }

    public static String cifrar(String numero) throws NegocioException {
        try {
            // Los primeros 12 bytes son un valor aleatorio (nonce) que también se guarda.
            byte[] nonce = new byte[12];
            azar.nextBytes(nonce);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, CLAVE, new GCMParameterSpec(128, nonce));
            byte[] cifrado = cifrador.doFinal(numero.getBytes(StandardCharsets.UTF_8));
            byte[] datos = new byte[nonce.length + cifrado.length];
            System.arraycopy(nonce, 0, datos, 0, nonce.length);
            System.arraycopy(cifrado, 0, datos, nonce.length, cifrado.length);
            return "AESGCM$" + Base64.getEncoder().encodeToString(datos);
        } catch (GeneralSecurityException e) {
            throw new NegocioException("No se pudo proteger el número del método.", e);
        }
    }

    public static String descifrar(String guardado) throws NegocioException {
        try {
            byte[] datos = Base64.getDecoder().decode(guardado.substring("AESGCM$".length()));
            Cipher descifrador = Cipher.getInstance("AES/GCM/NoPadding");
            descifrador.init(Cipher.DECRYPT_MODE, CLAVE, new GCMParameterSpec(128, datos, 0, 12));
            byte[] numero = descifrador.doFinal(datos, 12, datos.length - 12);
            return new String(numero, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new NegocioException("No se pudo leer el número protegido del método.", e);
        }
    }

    public static String enmascarar(String numero) {
        return "**** " + numero.substring(numero.length() - 4);
    }
}
