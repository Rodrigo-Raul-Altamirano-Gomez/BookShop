package me.Model.dto.solicitud;

import me.Model.dto.opinion.DatosResenaDTO;

// Consulta de referencia desde Mis solicitudes y contenido original de la comparacion administrativa.
// Identidad actual por referencia; puntuacion/comentario son los guardados al reportar, incluso tras editar.
// Sin votos ni acciones sobre la resena; consultar requiere acceso a la solicitud correspondiente.
public record ResenaReportadaDTO(
        int idResena,
        String nombreUsuario,
        String tituloLibro,
        DatosResenaDTO contenidoOriginal
) {
}
