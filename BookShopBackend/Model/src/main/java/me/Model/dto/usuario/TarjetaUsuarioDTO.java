package me.Model.dto.usuario;

import me.Model.dto.ImagenDTO;

// Identidad compartida por perfil, solicitante y documentos. Imagen puede omitirse donde no se muestra.
public record TarjetaUsuarioDTO(
        int id,
        String nombre,
        String email,
        ImagenDTO imagen
) {
}
