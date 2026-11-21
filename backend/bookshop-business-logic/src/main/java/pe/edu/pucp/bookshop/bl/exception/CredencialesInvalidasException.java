package pe.edu.pucp.bookshop.bl.exception;

// El correo o la contraseña no coinciden
public class CredencialesInvalidasException extends BusinessLogicException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
