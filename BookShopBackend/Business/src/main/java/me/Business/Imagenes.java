package me.Business;

import java.net.URI;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.ImagenDTO;

public final class Imagenes {
    private Imagenes() { }
    public static void validar(ImagenDTO imagen) throws ValidacionException {
        if (imagen == null) return;
        String url = imagen.url();
        if (url == null || url.isBlank() || url.length() > 2048)
            throw new ValidacionException("imagen", "Indica una URL de imagen de hasta 2048 caracteres.");
        if (url.startsWith("/images/") && !url.contains("..")) return;
        try {
            URI direccion = URI.create(url);
            if (("https".equalsIgnoreCase(direccion.getScheme()) || "http".equalsIgnoreCase(direccion.getScheme())) && direccion.getHost() != null) return;
        } catch (IllegalArgumentException e) { }
        throw new ValidacionException("imagen", "La imagen debe ser una URL HTTP o HTTPS válida.");
    }
}
