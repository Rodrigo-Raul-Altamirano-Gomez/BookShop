package pe.edu.pucp.bookshop.bl.exception;

import java.sql.SQLException;

// Falla el acceso a la base de datos
public class PersistenciaException extends BusinessLogicException {

    public PersistenciaException(SQLException causa) {
        super("No se pudo completar la operación. Inténtalo de nuevo.", causa);
    }
}
