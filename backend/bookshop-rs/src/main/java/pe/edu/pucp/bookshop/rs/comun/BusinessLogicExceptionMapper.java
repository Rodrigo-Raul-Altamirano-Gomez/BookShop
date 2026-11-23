package pe.edu.pucp.bookshop.rs.comun;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.CredencialesInvalidasException;
import pe.edu.pucp.bookshop.bl.exception.CuentaDesactivadaException;
import pe.edu.pucp.bookshop.bl.exception.DatosCambiadosException;
import pe.edu.pucp.bookshop.bl.exception.NoEncontradoException;
import pe.edu.pucp.bookshop.bl.exception.OperacionNoPermitidaException;
import pe.edu.pucp.bookshop.bl.exception.PagoRechazadoException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.exception.RegistroDuplicadoException;

// Traduce cada familia de excepción de negocio a su código HTTP; el mensaje va como texto para que el front lo muestre
@Provider
public class BusinessLogicExceptionMapper implements ExceptionMapper<BusinessLogicException> {

    @Override
    public Response toResponse(BusinessLogicException ex) {
        int estado = 400;
        if (ex instanceof CredencialesInvalidasException) estado = 401;
        if (ex instanceof CuentaDesactivadaException) estado = 403;
        if (ex instanceof NoEncontradoException) estado = 404;
        if (ex instanceof RegistroDuplicadoException) estado = 409;
        if (ex instanceof PagoRechazadoException) estado = 402;
        if (ex instanceof DatosCambiadosException) estado = 412;
        if (ex instanceof OperacionNoPermitidaException) estado = 422;
        if (ex instanceof PersistenciaException) {
            estado = 500;
            ex.getCause().printStackTrace();
        }
        return Response.status(estado).entity(ex.getMessage()).type("text/plain; charset=UTF-8").build();
    }
}
