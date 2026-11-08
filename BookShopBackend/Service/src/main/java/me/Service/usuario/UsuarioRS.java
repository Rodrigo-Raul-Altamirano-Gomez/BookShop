package me.Service.usuario;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ImagenDTO;
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
    private HttpServletRequest request;

    public record Clave(String contraseniaActual) { }

    @POST
    @Path("registro")
    public AccesoUsuarioDTO registrar(RegistroUsuarioDTO datos) throws NegocioException {
        comprobarSinSesion();
        AccesoUsuarioDTO acceso = servicios.usuario().registrar(datos);
        Sesion.abrir(request, acceso);
        return acceso;
    }

    @POST
    @Path("sesion")
    public AccesoUsuarioDTO iniciarSesion(CredencialesDTO datos) throws NegocioException {
        comprobarSinSesion();
        AccesoUsuarioDTO acceso = servicios.usuario().iniciarSesion(datos);
        Sesion.abrir(request, acceso);
        return acceso;
    }

    @POST
    @Path("reactivacion")
    public AccesoUsuarioDTO reactivar(CredencialesDTO datos) throws NegocioException {
        comprobarSinSesion();
        AccesoUsuarioDTO acceso = servicios.usuario().reactivar(datos);
        Sesion.abrir(request, acceso);
        return acceso;
    }

    @GET
    public PerfilUsuarioDTO perfil() throws NegocioException {
        return servicios.usuario().obtenerPerfil(Sesion.exigir(request));
    }

    @PUT
    public PerfilUsuarioDTO modificar(DatosPerfilDTO datos) throws NegocioException {
        return servicios.usuario().modificarPerfil(Sesion.exigir(request), datos);
    }

    @PUT
    @Path("imagen")
    public PerfilUsuarioDTO imagen(ImagenDTO datos) throws NegocioException {
        return servicios.usuario().cambiarImagen(Sesion.exigir(request), datos);
    }

    @PUT
    @Path("contrasenia")
    public void contrasenia(CambioContraseniaDTO datos) throws NegocioException {
        servicios.usuario().cambiarContrasenia(Sesion.exigir(request), datos);
    }

    @DELETE
    @Path("sesion")
    public void cerrarSesion() throws NegocioException {
        try {
            Sesion.registrarTiempo(request, servicios.usuario());
        } finally {
            Sesion.invalidar(request);
        }
    }

    @POST
    @Path("desactivacion")
    public void desactivar(Clave datos) throws NegocioException {
        int actor = Sesion.exigir(request);
        if (datos == null) throw new BadRequestException("Falta la contraseña actual.");
        // Verifica antes de contabilizar el tiempo; desactivar vuelve a comprobar la contraseña.
        PerfilUsuarioDTO perfil = servicios.usuario().obtenerPerfil(actor);
        servicios.usuario().iniciarSesion(new CredencialesDTO(perfil.usuario().email(), datos.contraseniaActual()));
        Sesion.registrarTiempo(request, servicios.usuario());
        servicios.usuario().desactivar(actor, datos.contraseniaActual());
        Sesion.invalidar(request);
    }

    private void comprobarSinSesion() {
        if (Sesion.actor(request) != 0) throw new ClientErrorException("Cierra la sesión actual primero.", 409);
    }
}
