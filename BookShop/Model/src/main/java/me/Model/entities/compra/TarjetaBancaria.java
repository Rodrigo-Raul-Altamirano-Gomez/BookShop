package me.Model.entities.compra;

import java.time.LocalDate;
import me.Model.entities.usuario.Usuario;

public class TarjetaBancaria extends MetodoPago {
    private String numeroTarjetaCifrado;
    private TipoTarjeta tipoTarjeta;

    public TarjetaBancaria() {
    }

    public TarjetaBancaria(int id, String titular, String proveedor, Usuario usuario, String alias,
                           String numeroTarjetaCifrado, LocalDate fechaVencimiento, TipoTarjeta tipoTarjeta) {
        super(id, titular, proveedor, usuario, alias, fechaVencimiento);
        this.numeroTarjetaCifrado = numeroTarjetaCifrado;
        this.tipoTarjeta = tipoTarjeta;
    }

    public String getNumeroTarjetaCifrado() {
        return numeroTarjetaCifrado;
    }

    public void setNumeroTarjetaCifrado(String numeroTarjetaCifrado) {
        this.numeroTarjetaCifrado = numeroTarjetaCifrado;
    }

    public TipoTarjeta getTipoTarjeta() {
        return tipoTarjeta;
    }

    public void setTipoTarjeta(TipoTarjeta tipoTarjeta) {
        this.tipoTarjeta = tipoTarjeta;
    }
}
