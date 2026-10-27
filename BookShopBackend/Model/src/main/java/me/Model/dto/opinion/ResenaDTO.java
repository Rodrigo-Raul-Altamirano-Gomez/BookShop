package me.Model.dto.opinion;

import java.time.LocalDateTime;
import me.Model.dto.ImagenDTO;

// Libro: foto del usuario; Mis resenas: portada; administracion: ambas. Imagenes omitidas son nulas.
// Voto propio solo para el listado publico interactivo. Activo permite consultar referencias retiradas.
// No expone correo ni auditoria; las acciones dependen del contexto y se autorizan en negocio.
public record ResenaDTO(
        int id,
        int idLibro,
        String tituloLibro,
        ImagenDTO portadaLibro,
        String nombreUsuario,
        ImagenDTO imagenUsuario,
        int puntuacion,
        String comentario,
        LocalDateTime fecha,
        int cantidadVotosUtiles,
        boolean votoUtilPropio,
        boolean activo
) {
}
