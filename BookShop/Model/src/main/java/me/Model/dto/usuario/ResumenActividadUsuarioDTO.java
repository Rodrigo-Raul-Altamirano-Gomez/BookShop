package me.Model.dto.usuario;

import java.math.BigDecimal;
import java.time.Duration;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;

// Gasto valido del mes, incluido envio; tiempo acumulado en la aplicacion.
// Sin compras, las tres referencias quedan null. El PDF utiliza sus nombres sin cargar imagenes.
public record ResumenActividadUsuarioDTO(
        TarjetaUsuarioDTO usuario,
        boolean activo,
        BigDecimal totalGastado,
        Duration tiempoTotal,
        TarjetaReferenciaDTO categoriaMasComprada,
        TarjetaReferenciaDTO autorMasComprado,
        TarjetaReferenciaDTO editorialMasComprada
) {
}
