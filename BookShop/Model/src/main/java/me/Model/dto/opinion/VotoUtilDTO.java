package me.Model.dto.opinion;

// Resultado de marcar/retirar un voto: actualiza el contador y el icono sin recargar imagenes/comentario.
// No representa el registro persistido de VotoUtil ni recibe el usuario desde el formulario.
public record VotoUtilDTO(
        int idResena,
        int cantidadVotosUtiles,
        boolean votoUtilPropio
) {
}
