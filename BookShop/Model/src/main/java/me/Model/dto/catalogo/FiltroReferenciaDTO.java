package me.Model.dto.catalogo;

// Nombre se usa para buscar opciones; el listado publico solo utiliza orden y paginacion.
public record FiltroReferenciaDTO(
        String nombre,
        boolean soloActivos,
        OrdenReferencia orden,
        int pagina,
        int tamanioPagina
) {
    public FiltroReferenciaDTO() {
        this(null, true, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12);
    }
}
