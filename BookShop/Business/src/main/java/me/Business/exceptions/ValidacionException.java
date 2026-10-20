package me.Business.exceptions;

public class ValidacionException extends NegocioException {
    private final String campo;
    public ValidacionException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }
    public String getCampo() { return campo; }
}
