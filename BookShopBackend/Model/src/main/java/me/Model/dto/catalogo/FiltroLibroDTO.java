package me.Model.dto.catalogo;

import java.math.BigDecimal;
import java.util.List;

// Precio minimo/maximo corresponden al precio final con descuento, antes de cupon y envio.
// El ambito de una referencia o lista personal se recibe aparte y no se borra al limpiar estos filtros.
public record FiltroLibroDTO(
        String titulo,
        List<Integer> idsCategorias,
        List<Integer> idsAutores,
        List<Integer> idsEditoriales,
        List<String> idiomas,
        BigDecimal precioMinimo,
        BigDecimal precioMaximo,
        boolean soloActivos,
        boolean soloNuevos,
        OrdenLibro orden,
        int pagina,
        int tamanioPagina
) {
    public FiltroLibroDTO {
        idsCategorias = idsCategorias == null ? List.of() : List.copyOf(idsCategorias);
        idsAutores = idsAutores == null ? List.of() : List.copyOf(idsAutores);
        idsEditoriales = idsEditoriales == null ? List.of() : List.copyOf(idsEditoriales);
        idiomas = idiomas == null ? List.of() : List.copyOf(idiomas);
    }

    public FiltroLibroDTO() {
        this(null, List.of(), List.of(), List.of(), List.of(), null, null, true, false,
                OrdenLibro.MAS_RECIENTES, 1, 12);
    }
}
