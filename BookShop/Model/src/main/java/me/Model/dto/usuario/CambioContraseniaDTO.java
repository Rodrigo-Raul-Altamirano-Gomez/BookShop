package me.Model.dto.usuario;

// Entrada de seguridad; las contrasenias no forman parte de las respuestas.
public record CambioContraseniaDTO(
        String contraseniaActual,
        String contraseniaNueva,
        String confirmacionContrasenia
) {
}
