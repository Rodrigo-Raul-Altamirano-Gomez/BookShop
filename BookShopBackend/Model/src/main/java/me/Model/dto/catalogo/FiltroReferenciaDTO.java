package me.Model.dto.catalogo;

// Busca por nombre dentro del tipo de referencia indicado por la operación.
public record FiltroReferenciaDTO(String nombre, boolean soloActivos, OrdenReferencia orden, int pagina, int
    tamanioPagina) {

    public static FiltroReferenciaDTO porDefecto() {
        return new FiltroReferenciaDTO(null, true, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12);
    }
}
