package me.Business.exceptions;

public class PersistenciaException extends NegocioException {
    public PersistenciaException(String mensaje) { super(mensaje); }
    public PersistenciaException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
