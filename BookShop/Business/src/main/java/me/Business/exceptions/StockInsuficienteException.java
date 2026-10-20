package me.Business.exceptions;

public class StockInsuficienteException extends NegocioException {
    public StockInsuficienteException(String mensaje) { super(mensaje); }
}
