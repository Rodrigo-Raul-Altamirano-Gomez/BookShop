package me.Model.dto.catalogo;

// Nombre busca por el inicio del nombre en listados y opciones.
public record FiltroReferenciaDTO(String nombre, boolean soloActivos, OrdenReferencia orden, int pagina, int
    tamanioPagina) {

    public static FiltroReferenciaDTO porDefecto() {
        return new FiltroReferenciaDTO(null, true, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12);
    }
}
