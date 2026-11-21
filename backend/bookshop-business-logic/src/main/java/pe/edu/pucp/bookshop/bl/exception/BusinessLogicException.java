package pe.edu.pucp.bookshop.bl.exception;

// Base de las excepciones de negocio. El mensaje es el texto que verá el usuario.
public class BusinessLogicException extends Exception {

    public BusinessLogicException(String mensaje) {
        super(mensaje);
    }

    public BusinessLogicException(String mensaje, Exception causa) {
        super(mensaje, causa);
    }
}
