package me.Model.dto.usuario;

import me.Model.entities.usuario.Rol;

// Respuesta de registro, login o reactivacion tras comprobar las credenciales.
// Si requiereReactivacion es true, se ofrece esa accion sin abrir una sesion activa.
public record AccesoUsuarioDTO(
        int id,
        String nombre,
        Rol rol,
        boolean requiereReactivacion
) {
}
