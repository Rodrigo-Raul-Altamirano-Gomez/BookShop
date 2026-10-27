package me.Model.dto;

// Contenido binario y formato (image/jpeg o image/png); la validacion corresponde a negocio.
public record ImagenDTO(
        byte[] contenido,
        String tipoMime
) {
    public ImagenDTO {
        contenido = contenido == null ? null : contenido.clone();
    }

    @Override
    public byte[] contenido() {
        return contenido == null ? null : contenido.clone();
    }
}
