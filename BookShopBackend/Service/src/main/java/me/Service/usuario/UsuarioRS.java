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
        if (Sesion.obtenerIdUsuario(peticion) != 0)
            throw new ClientErrorException("Cierra la sesión actual primero.", 409);
        AccesoUsuarioDTO acceso = servicios.getUsuarioBO().registrar(datos);
        Sesion.iniciar(peticion, acceso);
        return acceso;
    }

    @POST
    @Path("sesion")
    public AccesoUsuarioDTO iniciarSesion(CredencialesDTO datos) throws NegocioException {
        if (Sesion.obtenerIdUsuario(peticion) != 0)
            throw new ClientErrorException("Cierra la sesión actual primero.", 409);
        AccesoUsuarioDTO acceso = servicios.getUsuarioBO().iniciarSesion(datos);
        Sesion.iniciar(peticion, acceso);
        return acceso;
    }

    @POST
    @Path("reactivacion")
    public AccesoUsuarioDTO reactivar(CredencialesDTO datos) throws NegocioException {
        if (Sesion.obtenerIdUsuario(peticion) != 0)
            throw new ClientErrorException("Cierra la sesión actual primero.", 409);
        AccesoUsuarioDTO acceso = servicios.getUsuarioBO().reactivar(datos);
        Sesion.iniciar(peticion, acceso);
        return acceso;
    }

    @GET
    public PerfilUsuarioDTO perfil() throws NegocioException {
        return servicios.getUsuarioBO().obtenerPerfil(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()));
    }

    @PUT
    public PerfilUsuarioDTO modificar(DatosPerfilDTO datos) throws NegocioException {
        return servicios.getUsuarioBO().modificarPerfil(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), datos);
    }

    @PUT
    @Path("imagen")
    public PerfilUsuarioDTO imagen(@QueryParam("url") String url) throws NegocioException {
        return servicios.getUsuarioBO().cambiarImagen(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), url);
    }

    @PUT
    @Path("contrasenia")
    public void contrasenia(CambioContraseniaDTO datos) throws NegocioException {
        servicios.getUsuarioBO().cambiarContrasenia(Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO()), datos);
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
        int idUsuario = Sesion.exigirIdUsuario(peticion, servicios.getUsuarioBO());
        if (datos == null)
            throw new BadRequestException("Falta la contraseña actual.");
        // Verifica antes de contabilizar el tiempo; desactivar vuelve a comprobar la contraseña.
        PerfilUsuarioDTO perfil = servicios.getUsuarioBO().obtenerPerfil(idUsuario);
        servicios.getUsuarioBO().iniciarSesion(new CredencialesDTO(perfil.usuario().email(),
            datos.contraseniaActual()));
        Sesion.registrarTiempoDeUso(peticion, servicios.getUsuarioBO());
        servicios.getUsuarioBO().desactivar(idUsuario, datos.contraseniaActual());
        Sesion.cerrar(peticion);
    }
}
