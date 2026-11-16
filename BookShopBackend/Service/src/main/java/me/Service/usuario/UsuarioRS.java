package me.Service.usuario;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.usuario.AccesoUsuarioDTO;
import me.Model.dto.usuario.CambioContraseniaDTO;
import me.Model.dto.usuario.CredencialesDTO;
import me.Model.dto.usuario.DatosPerfilDTO;
import me.Model.dto.usuario.PerfilUsuarioDTO;
import me.Model.dto.usuario.RegistroUsuarioDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("cuenta")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class UsuarioRS {

    @Inject
    private Servicios servicios;

    @Context
    private HttpServletRequest peticion;

    public record DesactivacionCuenta(String contraseniaActual) {
    }

    @POST
    @Path("registro")
    public AccesoUsuarioDTO registrar(RegistroUsuarioDTO datos) throws NegocioException {
        AccesoUsuarioDTO acceso = servicios.getUsuarioBO().registrar(datos);
        Sesion.iniciar(peticion, acceso);
        return acceso;
    }

    @POST
    @Path("sesion")
    public AccesoUsuarioDTO iniciarSesion(CredencialesDTO datos) throws NegocioException {
        AccesoUsuarioDTO acceso = servicios.getUsuarioBO().iniciarSesion(datos);
        Sesion.iniciar(peticion, acceso);
        return acceso;
    }

    @POST
    @Path("reactivacion")
    public AccesoUsuarioDTO reactivar(CredencialesDTO datos) throws NegocioException {
        AccesoUsuarioDTO acceso = servicios.getUsuarioBO().reactivar(datos);
        Sesion.iniciar(peticion, acceso);
        return acceso;
    }

    @GET
    public PerfilUsuarioDTO perfil() throws NegocioException {
        return servicios.getUsuarioBO().obtenerPerfil(Sesion.exigirIdUsuario(peticion));
    }

    @PUT
    public PerfilUsuarioDTO modificar(DatosPerfilDTO datos) throws NegocioException {
        return servicios.getUsuarioBO().modificarPerfil(Sesion.exigirIdUsuario(peticion), datos);
    }

    @PUT
    @Path("imagen")
    public PerfilUsuarioDTO imagen(@QueryParam("url") String url) throws NegocioException {
        return servicios.getUsuarioBO().cambiarImagen(Sesion.exigirIdUsuario(peticion), url);
    }

    @PUT
    @Path("contrasenia")
    public void contrasenia(CambioContraseniaDTO datos) throws NegocioException {
        servicios.getUsuarioBO().cambiarContrasenia(Sesion.exigirIdUsuario(peticion), datos);
    }

    @DELETE
    @Path("sesion")
    public void cerrarSesion() throws NegocioException {
        try {
            Sesion.registrarTiempoDeUso(peticion, servicios.getUsuarioBO());
        } finally {
            Sesion.cerrar(peticion);
        }
    }

    @POST
    @Path("desactivacion")
    public void desactivar(DesactivacionCuenta datos) throws NegocioException {
        int idUsuario = Sesion.exigirIdUsuario(peticion);
        if (datos == null)
            throw new BadRequestException("Falta la contraseña actual.");
        servicios.getUsuarioBO().desactivar(idUsuario, datos.contraseniaActual());
        try {
            // La cuenta inactiva conserva su historial y puede acumular el tiempo de esta sesión.
            Sesion.registrarTiempoDeUso(peticion, servicios.getUsuarioBO());
        } finally {
            Sesion.cerrar(peticion);
        }
    }
}
