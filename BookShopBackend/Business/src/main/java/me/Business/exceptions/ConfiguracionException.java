package me.Business.exceptions;

public class ConfiguracionException extends NegocioException {
    public ConfiguracionException(String mensaje) { super(mensaje); }
    public ConfiguracionException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
