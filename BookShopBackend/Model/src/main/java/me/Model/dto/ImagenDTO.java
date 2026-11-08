package me.Model.dto;

// Contenido binario y formato (image/jpeg o image/png); la validacion corresponde a negocio.
public record ImagenDTO(
        byte[] contenido,
        String tipoMime
) {
}
