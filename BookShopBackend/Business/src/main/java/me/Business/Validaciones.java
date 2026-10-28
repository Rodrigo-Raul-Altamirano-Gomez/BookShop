package me.Business;

import java.util.Iterator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.ImagenDTO;

public final class Validaciones {
    private Validaciones() { }

    public static String texto(String valor, String campo, int maximo) throws ValidacionException {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty()) throw new ValidacionException(campo, "Este campo es obligatorio.");
        if (limpio.length() > maximo)
            throw new ValidacionException(campo, "El máximo es " + maximo + " caracteres.");
        return limpio;
    }

    public static String opcional(String valor, String campo, int maximo) throws ValidacionException {
        if (valor == null || valor.isBlank()) return null;
        return texto(valor, campo, maximo);
    }

    public static void pagina(int pagina, int tamanio) throws ValidacionException {
        if (pagina < 1) throw new ValidacionException("pagina", "La página debe ser positiva.");
        if (tamanio < 1 || (long) (pagina - 1) * tamanio > Integer.MAX_VALUE)
            throw new ValidacionException("tamanioPagina", "El tamaño y desplazamiento de página no son válidos.");
    }

    public static void imagen(ImagenDTO imagen) throws ValidacionException {
        if (imagen == null) return;
        byte[] bytes = imagen.contenido();
        if (bytes == null) {
            if (imagen.tipoMime() != null) throw new ValidacionException("imagen", "Falta el contenido de la imagen.");
            return;
        }
        if (bytes.length == 0 || bytes.length > 5 * 1024 * 1024)
            throw new ValidacionException("imagen", "La imagen debe contener datos y pesar como máximo 5 MiB.");
        String mime = imagen.tipoMime();
        if (!"image/jpeg".equals(mime) && !"image/png".equals(mime))
            throw new ValidacionException("imagen", "Solo se admiten JPEG y PNG.");
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw new ValidacionException("imagen", "El archivo no es una imagen válida.");
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                String formato = lector.getFormatName().toLowerCase(Locale.ROOT);
                if (!(formato.equals("png") && mime.equals("image/png")
                        || (formato.equals("jpeg") || formato.equals("jpg")) && mime.equals("image/jpeg"))
                        || lector.read(0) == null)
                    throw new ValidacionException("imagen", "El contenido no corresponde al formato indicado.");
            } finally { lector.dispose(); }
        } catch (IOException | RuntimeException e) {
            throw new ValidacionException("imagen", "No se pudo leer el contenido de la imagen.");
        }
    }
}
