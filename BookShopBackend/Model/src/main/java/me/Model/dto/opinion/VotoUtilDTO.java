package me.Model.dto.opinion;

// Resultado de marcar/retirar un voto: actualiza el contador y el icono sin recargar imagenes/comentario.
// Resultado de marcar o retirar un voto útil.
public record VotoUtilDTO(int idResena, int cantidadVotosUtiles, boolean votoUtilPropio) {
}
