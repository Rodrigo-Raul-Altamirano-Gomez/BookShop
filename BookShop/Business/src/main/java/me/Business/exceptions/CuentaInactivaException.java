package me.Business.exceptions;

public class CuentaInactivaException extends NegocioException {
    public CuentaInactivaException(String mensaje) { super(mensaje); }
}
