package me.Model.dto.catalogo;

public record DetalleReferenciaDTO(
        TarjetaReferenciaDTO referencia,
        String introduccion,
        boolean activo,
        boolean seguido
) {
}
