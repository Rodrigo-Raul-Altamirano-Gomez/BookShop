package me.Business.exceptions;

public class ProteccionDatosException extends NegocioException {
    public ProteccionDatosException(String mensaje) { super(mensaje); }
    public ProteccionDatosException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
