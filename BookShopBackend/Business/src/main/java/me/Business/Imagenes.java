package me.Business;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.ImagenDTO;

// La lectura del archivo queda separada de los formularios que lo utilizan.
public final class Imagenes {
    private Imagenes() { }

    public static void validar(ImagenDTO imagen) throws ValidacionException {
        if (imagen == null) return;
        byte[] contenido = imagen.contenido();
        String tipo = imagen.tipoMime();
        if (contenido == null && tipo == null) return;
        if (contenido == null || contenido.length == 0 || contenido.length > 5 * 1024 * 1024)
            throw new ValidacionException("imagen", "La imagen debe contener datos y pesar como máximo 5 MiB.");
        if (!"image/png".equals(tipo) && !"image/jpeg".equals(tipo))
            throw new ValidacionException("imagen", "Solo se admiten JPEG y PNG.");

        boolean png = contenido.length >= 8 && contenido[0] == (byte) 137
                && contenido[1] == 80 && contenido[2] == 78 && contenido[3] == 71
                && contenido[4] == 13 && contenido[5] == 10 && contenido[6] == 26 && contenido[7] == 10;
        boolean jpeg = contenido.length >= 3 && contenido[0] == (byte) 255
                && contenido[1] == (byte) 216 && contenido[2] == (byte) 255;
        if (("image/png".equals(tipo) && !png) || ("image/jpeg".equals(tipo) && !jpeg))
            throw new ValidacionException("imagen", "El contenido no corresponde al formato indicado.");
        try {
            if (ImageIO.read(new ByteArrayInputStream(contenido)) == null)
                throw new ValidacionException("imagen", "El archivo no es una imagen válida.");
        } catch (IOException e) {
            throw new ValidacionException("imagen", "No se pudo leer el contenido de la imagen.");
        }
    }
}
