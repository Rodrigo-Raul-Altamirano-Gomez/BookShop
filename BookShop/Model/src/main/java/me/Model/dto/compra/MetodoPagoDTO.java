package me.Model.dto.compra;

import java.time.LocalDate;
import me.Model.entities.compra.TipoTarjeta;

// Consulta y precarga de edicion: solo mascara. Los vencidos siguen visibles en Datos de compra.
// En billetera tipoTarjeta es nulo; fechaVencimiento contiene su vigencia calculada.
public record MetodoPagoDTO(
        int id,
        boolean tarjetaBancaria,
        String titular,
        String proveedor,
        String alias,
        String numeroEnmascarado,
        TipoTarjeta tipoTarjeta,
        LocalDate fechaVencimiento,
        boolean predeterminado,
        boolean vigente
) {
}
