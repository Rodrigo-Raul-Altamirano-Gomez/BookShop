package me.App;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import me.Business.ProteccionDatos;
import me.Business.exceptions.NegocioException;

// Ejemplo didactico de ProteccionDatos: no usa la base de datos ni la clave real del servidor.
// Muestra por que compra-demo.sql necesita valores generados en Java y no textos escritos a mano.
public class DemoProteccion {
    public static void main(String[] args) throws Exception {
        // Clave temporal solo para este ejemplo; la del servidor esta en BookShopBackend/config.
        Path claveDemo = Files.createTempDirectory("bookshop-demo").resolve("clave-demo.key");
        System.setProperty("bookshop.clave.file", claveDemo.toString());

        titulo("1. CONTRASEÑAS: hash con sal (no se puede revertir)");
        String contrasenia = "Demo12345";
        String hash1 = ProteccionDatos.hash(contrasenia);
        String hash2 = ProteccionDatos.hash(contrasenia);
        System.out.println("Contraseña escrita por el usuario : " + contrasenia);
        System.out.println("Lo que se guarda (1.er registro)  : " + hash1);
        System.out.println("Lo que se guarda (2.º registro)   : " + hash2);
        System.out.println("¿Son iguales?                     : " + hash1.equals(hash2)
                + "  <- la sal aleatoria cambia el resultado");
        String[] partes = hash1.split("\\$");
        System.out.println("Partes: algoritmo=" + partes[0] + ", iteraciones=" + partes[1]
                + ", sal=" + partes[2] + ", huella=" + partes[3].substring(0, 12) + "...");
        System.out.println("verificar(\"Demo12345\")  -> " + ProteccionDatos.verificar("Demo12345", hash1));
        System.out.println("verificar(\"demo12345\")  -> " + ProteccionDatos.verificar("demo12345", hash1));
        System.out.println("verificar(\"Demo12345\") contra el texto '${HASH}' -> "
                + ProteccionDatos.verificar("Demo12345", "${HASH}")
                + "  <- por eso Ana no puede iniciar sesión");

        titulo("2. TARJETAS: cifrado AES-GCM (se revierte solo con la clave)");
        String tarjeta = "4111111111111111";
        String cifrado1 = ProteccionDatos.cifrar(tarjeta);
        String cifrado2 = ProteccionDatos.cifrar(tarjeta);
        System.out.println("Número de tarjeta             : " + tarjeta);
        System.out.println("Lo que se guarda (1.ª vez)    : " + cifrado1);
        System.out.println("Lo que se guarda (2.ª vez)    : " + cifrado2);
        System.out.println("¿Son iguales?                 : " + cifrado1.equals(cifrado2)
                + "  <- el nonce aleatorio cambia el resultado");
        System.out.println("descifrar(1.ª vez)            : " + ProteccionDatos.descifrar(cifrado1));
        System.out.println("Lo que ve el usuario          : " + ProteccionDatos.enmascarar(tarjeta));

        titulo("3. ¿Qué pasa si algo está mal?");
        intentar("Texto de relleno '${CIFRADO}'", "${CIFRADO}");
        intentar("Cifrado modificado (1 byte cambiado)", alterar(cifrado1));
        // Otra clave = otro servidor: no puede leer lo que cifró el primero.
        System.setProperty("bookshop.clave.file", claveDemo.resolveSibling("otra-clave.key").toString());
        intentar("Mismo cifrado, pero con OTRA clave", cifrado1);

        titulo("Conclusión");
        System.out.println("- La base solo guarda 'PBKDF2$...' y 'AESGCM$...'; nunca la contraseña ni la tarjeta.");
        System.out.println("- Esos textos los genera Java; no se pueden escribir a mano en un .sql.");
        System.out.println("- La clave debe ser la misma en la consola y en GlassFish, y no debe perderse.");
    }

    private static void intentar(String caso, String valor) {
        try {
            ProteccionDatos.descifrar(valor);
            System.out.println(caso + " -> se pudo leer (no debería pasar)");
        } catch (NegocioException e) {
            System.out.println(caso + " -> ERROR: " + e.getMessage());
        }
    }

    private static String alterar(String cifrado) {
        byte[] datos = Base64.getDecoder().decode(cifrado.substring("AESGCM$".length()));
        datos[datos.length - 1] ^= 1;
        return "AESGCM$" + Base64.getEncoder().encodeToString(datos);
    }

    private static void titulo(String texto) {
        System.out.println("\n=== " + texto + " ===");
    }
}
