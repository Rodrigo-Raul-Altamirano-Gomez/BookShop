package me.Model.dto.compra;

public record DireccionEnvioDTO(
        int id,
        String alias,
        String calle,
        String distrito,
        String ciudad,
        String referencia,
        boolean predeterminada
) {
}
