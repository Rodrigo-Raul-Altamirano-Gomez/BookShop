package me.Model.dto.catalogo;

import java.math.BigDecimal;

// Consulta administrativa para precargar Datos, Precio y Stock. Las referencias son de solo lectura.
// Las tarjetas de referencia contienen id/nombre; no se cargan sus imagenes para este formulario.
public record GestionLibroDTO(
        int id,
        DatosLibroDTO datos,
        TarjetaReferenciaDTO autor,
        TarjetaReferenciaDTO categoria,
        TarjetaReferenciaDTO editorial,
        BigDecimal precioBase,
        int stockFisico
) {
}
