package me.Model.dto.usuario;

import java.time.LocalDate;
import me.Model.dto.ImagenDTO;

// Alta con nombre completo y foto opcional. El rol se determina por el correo, no por el formulario.
// Contraseña y confirmacion son entradas; la respuesta utiliza AccesoUsuarioDTO.
public record RegistroUsuarioDTO(
        String nombre,
        String email,
        LocalDate fechaNacimiento,
        String contrasenia,
        String confirmacionContrasenia,
        ImagenDTO imagen
) {
}
