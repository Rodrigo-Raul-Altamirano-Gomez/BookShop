package pe.edu.pucp.bookshop.bl.exception;

// Ya existe un registro que no puede repetirse
public class RegistroDuplicadoException extends BusinessLogicException {

    public RegistroDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
