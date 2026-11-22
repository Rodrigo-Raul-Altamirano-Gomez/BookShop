package pe.edu.pucp.bookshop.bl.exception;

// La operación no se permite por el estado del registro o por un límite (por ejemplo, cancelar o devolver un pedido)
public class OperacionNoPermitidaException extends BusinessLogicException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
