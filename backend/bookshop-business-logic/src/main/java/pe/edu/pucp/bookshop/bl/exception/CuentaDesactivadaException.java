package pe.edu.pucp.bookshop.bl.exception;

// La cuenta existe pero está desactivada
public class CuentaDesactivadaException extends BusinessLogicException {

    public CuentaDesactivadaException(String mensaje) {
        super(mensaje);
    }
}
