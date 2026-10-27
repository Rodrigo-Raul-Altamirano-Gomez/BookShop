package me.Model.dto.usuario;

import java.time.LocalDate;

// Mi perfil: identidad, foto y nacimiento. Rol y reactivacion pertenecen a la respuesta de acceso.
public record PerfilUsuarioDTO(
        TarjetaUsuarioDTO usuario,
        LocalDate fechaNacimiento
) {
}
