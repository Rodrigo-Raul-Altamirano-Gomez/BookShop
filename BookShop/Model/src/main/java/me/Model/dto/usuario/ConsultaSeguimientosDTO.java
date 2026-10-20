package me.Model.dto.usuario;

import me.Model.dto.catalogo.OrdenReferencia;

// Consulta de la pestaña elegida; MAS_RECIENTES usa la fecha del seguimiento.
// El usuario se obtiene del contexto autenticado; las respuestas reutilizan TarjetaReferenciaDTO.
public record ConsultaSeguimientosDTO(
        TipoSeguimiento tipo,
        OrdenReferencia orden,
        int pagina,
        int tamanioPagina
) {
    public ConsultaSeguimientosDTO() {
        this(TipoSeguimiento.AUTOR, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12);
    }
}
