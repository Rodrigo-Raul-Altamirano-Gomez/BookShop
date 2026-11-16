package me.Business.usuario;

import java.time.LocalDate;
import me.Business.exceptions.NegocioException;
import me.Business.ProteccionDatos;
import me.Model.dto.compra.MetodoPagoDTO;
import me.Model.entities.compra.BilleteraDigital;
import me.Model.entities.compra.MetodoPago;
import me.Model.entities.compra.TarjetaBancaria;
import me.Model.entities.compra.TipoTarjeta;

// Conversion interna del servidor, compartida por datos de compra e historial.
public final class MetodosPago {

    private MetodosPago() {
    }

    public static MetodoPagoDTO aDTO(MetodoPago metodo, LocalDate fecha) throws NegocioException {
        boolean tarjeta = metodo instanceof TarjetaBancaria;
        TipoTarjeta tipo = null;
        if (tarjeta)
            tipo = ((TarjetaBancaria) metodo).getTipoTarjeta();
        String mascara = ProteccionDatos.enmascarar(numeroCompleto(metodo));
        boolean vigente = metodo.validarEstado(fecha);
        return new MetodoPagoDTO(metodo.getId(), tarjeta, metodo.getTitular(), metodo.getProveedor(),
            metodo.getAlias(), mascara, tipo,
            metodo.getFechaVencimiento(), metodo.isPredeterminado() && vigente, vigente);
    }

    public static String numeroCompleto(MetodoPago metodo) throws NegocioException {
        String cifrado;
        if (metodo instanceof TarjetaBancaria) {
            TarjetaBancaria tarjeta = (TarjetaBancaria) metodo;
            cifrado = tarjeta.getNumeroTarjetaCifrado();
        } else {
            BilleteraDigital billetera = (BilleteraDigital) metodo;
            cifrado = billetera.getNumeroTelefonoCifrado();
        }
        return ProteccionDatos.descifrar(cifrado);
    }
}
