package pe.edu.pucp.bookshop.bl.exception;

// El pago simulado fue rechazado (ERS RF15.6)
public class PagoRechazadoException extends BusinessLogicException {
    public PagoRechazadoException(String mensaje) {
        super(mensaje);
    }
}
