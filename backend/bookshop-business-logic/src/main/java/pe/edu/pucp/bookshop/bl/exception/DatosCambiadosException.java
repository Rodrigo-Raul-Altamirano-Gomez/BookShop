package pe.edu.pucp.bookshop.bl.exception;

// Al pagar, algo cambió desde que se preparó la compra (precio, stock, cupón o envío). El mensaje lista los cambios, uno por línea.
public class DatosCambiadosException extends BusinessLogicException {
    public DatosCambiadosException(String mensaje) {
        super(mensaje);
    }
}
