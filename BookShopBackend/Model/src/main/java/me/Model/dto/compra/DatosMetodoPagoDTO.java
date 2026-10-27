package me.Model.dto.compra;

import java.time.LocalDate;
import me.Model.entities.compra.TipoTarjeta;

// Solo entrada. El id al editar se recibe aparte; tarjetaBancaria elige el subtipo al registrar.
// En edicion el subtipo no cambia y numero nulo/vacio conserva el numero guardado, nunca su mascara.
// En billetera, tipoTarjeta y fechaVencimiento son nulos; su vencimiento se determina en negocio.
public record DatosMetodoPagoDTO(
        boolean tarjetaBancaria,
        String titular,
        String proveedor,
        String alias,
        String numero,
        TipoTarjeta tipoTarjeta,
        LocalDate fechaVencimiento,
        boolean predeterminado
) {
}
