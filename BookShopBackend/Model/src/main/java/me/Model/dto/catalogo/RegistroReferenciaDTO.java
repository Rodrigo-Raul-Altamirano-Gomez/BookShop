package me.Model.dto.catalogo;

// Alta de autor, categoria o editorial. La operacion elegida determina el tipo de referencia.
public record RegistroReferenciaDTO(DatosReferenciaDTO datos, String imagen) {
}
