package me.Model.dto.catalogo;

import me.Model.dto.ImagenDTO;

// Alta de autor, categoria o editorial. La operacion elegida determina el tipo de referencia.
public record RegistroReferenciaDTO(
        DatosReferenciaDTO datos,
        ImagenDTO imagen
) {
}
