package me.Service;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;

// Convierte las excepciones en una respuesta JSON con un código HTTP y un mensaje para el usuario.
@Provider
public class Errores implements ExceptionMapper<Exception> {

    public record ErrorRespuesta(String mensaje) {
    }

    @Override
    public Response toResponse(Exception error) {
        int estado;
        String mensaje = error.getMessage();
        if (error instanceof PersistenciaException) {
            estado = 500;
            error.printStackTrace();
        } else if (error instanceof ValidacionException) {
            estado = 400;
        } else if (error instanceof AccesoDenegadoException) {
            estado = 403;
        } else if (error instanceof RecursoNoEncontradoException) {
            estado = 404;
        } else if (error instanceof NegocioException) {
            estado = 409;
        } else if (error instanceof WebApplicationException) {
            // Errores de JAX-RS: ruta inexistente, falta de sesión, JSON inválido, etc.
            estado = ((WebApplicationException) error).getResponse().getStatus();
        } else {
            estado = 500;
            mensaje = "No se pudo completar la operación.";
            error.printStackTrace();
        }
        return Response.status(estado).type(MediaType.APPLICATION_JSON)
            .entity(new ErrorRespuesta(mensaje)).build();
    }
}
