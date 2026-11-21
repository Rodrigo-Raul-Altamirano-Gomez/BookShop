package pe.edu.pucp.bookshop.bl.exception;

// Lo pedido no existe
public class NoEncontradoException extends BusinessLogicException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
