package me.Business;

import me.Business.exceptions.ValidacionException;

public final class Imagenes {
    private Imagenes() { }
    public static void validar(String url) throws ValidacionException {
        if (url == null) return;
        if (url.isBlank() || url.length() > 2048)
            throw new ValidacionException("imagen", "Indica una URL de imagen de hasta 2048 caracteres.");
        if (!url.startsWith("https://") && !url.startsWith("http://") && !url.startsWith("/images/"))
            throw new ValidacionException("imagen", "Use una URL HTTP o HTTPS para la imagen.");
    }
}
