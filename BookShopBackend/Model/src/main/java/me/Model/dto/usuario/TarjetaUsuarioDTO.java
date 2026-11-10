package me.Model.dto.usuario;


// Identidad compartida por perfil, solicitante y documentos. Imagen puede omitirse donde no se muestra.
public record TarjetaUsuarioDTO(
        int id,
        String nombre,
        String email,
        String imagen
) {
}
