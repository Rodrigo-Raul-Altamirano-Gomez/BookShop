package me.Model.entities.compra;

import java.time.LocalDate;
import me.Model.entities.usuario.Usuario;

public class BilleteraDigital extends MetodoPago {
    private String numeroTelefonoCifrado;

    public BilleteraDigital() {
    }

    public BilleteraDigital(int id, String titular, String proveedor, Usuario usuario, String alias,
                           String numeroTelefonoCifrado, LocalDate fechaVencimiento) {
        super(id, titular, proveedor, usuario, alias, fechaVencimiento);
        this.numeroTelefonoCifrado = numeroTelefonoCifrado;
    }

    public String getNumeroTelefonoCifrado() {
        return numeroTelefonoCifrado;
    }

    public void setNumeroTelefonoCifrado(String numeroTelefonoCifrado) {
        this.numeroTelefonoCifrado = numeroTelefonoCifrado;
    }
}
