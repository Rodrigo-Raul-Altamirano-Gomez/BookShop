package me.Model.dto.usuario;

// Entrada de login y reactivacion; la respuesta utiliza AccesoUsuarioDTO.
public record CredencialesDTO(
        String email,
        String contrasenia
) {
}
