package me.Business.usuario;

import java.time.LocalDate;
import me.Business.exceptions.NegocioException;
import me.Business.ProteccionDatos;
import me.Model.dto.compra.MetodoPagoDTO;
import me.Model.entities.compra.BilleteraDigital;
import me.Model.entities.compra.MetodoPago;
import me.Model.entities.compra.TarjetaBancaria;

// Conversion interna del servidor, compartida por datos de compra e historial.
public final class MetodosPago {
    private MetodosPago() {
    }

    public static MetodoPagoDTO aDTO(MetodoPago metodo, LocalDate fecha) throws NegocioException {
        if (metodo == null) return null;
        boolean tarjeta = metodo instanceof TarjetaBancaria;
        String cifrado = tarjeta ? ((TarjetaBancaria) metodo).getNumeroTarjetaCifrado()
                : ((BilleteraDigital) metodo).getNumeroTelefonoCifrado();
        String mascara = ProteccionDatos.enmascarar(ProteccionDatos.descifrar(cifrado));
        boolean vigente = metodo.validarEstado(fecha);
        return new MetodoPagoDTO(metodo.getId(), tarjeta, metodo.getTitular(), metodo.getProveedor(),
                metodo.getAlias(), mascara, tarjeta ? ((TarjetaBancaria) metodo).getTipoTarjeta() : null,
                metodo.getFechaVencimiento(), metodo.isPredeterminado() && vigente, vigente);
    }
}
