package pe.edu.pucp.bookshop.rs.usuario;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.usuario.UsuarioBL;
import pe.edu.pucp.bookshop.bl.usuario.impl.UsuarioBLImpl;
import pe.edu.pucp.bookshop.model.usuario.CambioContrasena;
import pe.edu.pucp.bookshop.model.usuario.Credenciales;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

@Path("usuarios")
@Produces("application/json")
@Consumes("application/json")
public class UsuarioRS {

    private UsuarioBL usuarioBL = new UsuarioBLImpl();

    @POST
    public Usuario registrar(Usuario usuario) throws BusinessLogicException {
        return usuarioBL.registrar(usuario);
    }

    @POST
    @Path("login")
    public Usuario login(Credenciales credenciales) throws BusinessLogicException {
        return usuarioBL.login(credenciales.correo(), credenciales.contrasena());
    }

    @PUT
    @Path("reactivar")
    public Usuario reactivar(Credenciales credenciales) throws BusinessLogicException {
        return usuarioBL.reactivar(credenciales.correo(), credenciales.contrasena());
    }

    @GET
    @Path("{id}")
    public Usuario obtener(@PathParam("id") int id) throws BusinessLogicException {
        return usuarioBL.obtener(id);
    }

    @PUT
    @Path("{id}")
    public Usuario modificar(@PathParam("id") int id, Usuario usuario) throws BusinessLogicException {
        usuario.setId(id);
        return usuarioBL.modificar(usuario);
    }

    @PUT
    @Path("{id}/contrasena")
    public void cambiarContrasena(@PathParam("id") int id, CambioContrasena cambio) throws BusinessLogicException {
        usuarioBL.cambiarContrasena(id, cambio);
    }

    @PUT
    @Path("{id}/desactivar")
    public void desactivar(@PathParam("id") int id) throws BusinessLogicException {
        usuarioBL.desactivar(id);
    }
}
