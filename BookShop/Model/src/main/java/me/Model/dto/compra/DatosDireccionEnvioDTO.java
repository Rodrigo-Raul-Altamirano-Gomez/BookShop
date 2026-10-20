package me.Model.dto.compra;

// Alta y edicion comparten campos; el id al editar y el propietario no pertenecen al formulario.
public record DatosDireccionEnvioDTO(
        String alias,
        String calle,
        String distrito,
        String ciudad,
        String referencia,
        boolean predeterminada
) {
}
