package me.Service;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.CondicionesCompraCambiaronException;
import me.Business.exceptions.ConfiguracionException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.CredencialesInvalidasException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.ProteccionDatosException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.StockInsuficienteException;
import me.Business.exceptions.ValidacionException;

@Provider
public class Errores implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(Errores.class.getName());

    public record ErrorRespuesta(String codigo, String mensaje, String campo) { }

    @Override
    public Response toResponse(Exception error) {
        int estado = 500;
        String codigo = "ERROR_INTERNO";
        String mensaje = "No se pudo completar la operación.";
        String campo = null;
        boolean interno = error instanceof PersistenciaException || error instanceof ProteccionDatosException
                || error instanceof ConfiguracionException;
        if (error instanceof NegocioException && !interno) {
            codigo = error.getClass().getSimpleName().replace("Exception", "");
            mensaje = error.getMessage();
            estado = 422;
            if (error instanceof ValidacionException validacion) {
                estado = 400;
                campo = validacion.getCampo();
            } else if (error instanceof CredencialesInvalidasException) estado = 401;
            else if (error instanceof AccesoDenegadoException || error instanceof CuentaInactivaException) estado = 403;
            else if (error instanceof RecursoNoEncontradoException) estado = 404;
            else if (error instanceof ConflictoException || error instanceof CondicionesCompraCambiaronException
                    || error instanceof StockInsuficienteException) estado = 409;
        } else if (error instanceof JsonbException
                || (error instanceof ProcessingException && error.getCause() instanceof JsonbException)) {
            estado = 400;
            codigo = "JSON_INVALIDO";
            mensaje = "El contenido JSON no es válido para esta operación.";
        } else if (error instanceof WebApplicationException web) {
            estado = web.getResponse().getStatus();
            codigo = "HTTP_" + estado;
            mensaje = switch (estado) {
                case 400 -> "La solicitud o sus parámetros no son válidos.";
                case 401 -> "Se requiere iniciar sesión.";
                case 403 -> "La solicitud no está permitida.";
                case 404 -> "El recurso no existe.";
                case 405 -> "El método HTTP no está permitido.";
                case 409 -> "La operación entra en conflicto con el estado actual.";
                case 413 -> "La solicitud supera el tamaño permitido.";
                case 415 -> "Se requiere contenido JSON.";
                default -> "No se pudo completar la solicitud.";
            };
        }
        if (estado >= 500) LOG.log(Level.SEVERE, "Error al atender la solicitud REST", error);
        Response.ResponseBuilder respuesta = Response.status(estado).type(MediaType.APPLICATION_JSON)
                .entity(new ErrorRespuesta(codigo, mensaje, campo));
        if (error instanceof WebApplicationException web) {
            // Conserva cabeceras de protocolo, por ejemplo Allow y WWW-Authenticate.
            for (String nombre : web.getResponse().getHeaders().keySet()) {
                if (!nombre.equalsIgnoreCase("Content-Type") && !nombre.equalsIgnoreCase("Content-Length")) {
                    List<Object> valores = web.getResponse().getHeaders().get(nombre);
                    for (Object valor : valores) {
                        respuesta.header(nombre, valor);
                    }
                }
            }
        }
        return respuesta.build();
    }
}
