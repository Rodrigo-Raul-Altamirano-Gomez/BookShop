package me.Model.dto.opinion;

// Listados sin filtros. La operacion determina libro, usuario propio o vista administrativa.
// La respuesta reutiliza ResultadoPaginadoDTO<ResenaDTO>; el bloque publico usa orden reciente.
public record ConsultaResenasDTO(
        OrdenResena orden,
        int pagina,
        int tamanioPagina
) {
    public ConsultaResenasDTO() {
        this(OrdenResena.MAS_RECIENTES, 1, 10);
    }
}
