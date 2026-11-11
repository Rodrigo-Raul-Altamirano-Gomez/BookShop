package me.Service;

import java.time.Duration;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.NotAuthorizedException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.usuario.UsuarioBO;
import me.Model.dto.usuario.AccesoUsuarioDTO;

public final class Sesion {

    private static final String ID_USUARIO = "bookshop.actor";
    private static final String INICIO_SESION = "bookshop.inicio";
    private Sesion() {
    }

    public static int obtenerIdUsuario(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion == null)
            return 0;
        Integer idUsuario = (Integer) sesion.getAttribute(ID_USUARIO);
        return idUsuario == null ? 0 : idUsuario;
    }

    public static int exigirIdUsuario(HttpServletRequest peticion, UsuarioBO usuarios) throws NegocioException {
        int idUsuario = obtenerIdUsuario(peticion);
        if (idUsuario <= 0)
            throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
        try {
            usuarios.comprobarActivo(idUsuario);
        } catch (CuentaInactivaException e) {
            cerrar(peticion);
            throw new NotAuthorizedException("La cuenta está desactivada.", "Sesion");
        }
        return idUsuario;
    }

    public static void iniciar(HttpServletRequest peticion, AccesoUsuarioDTO acceso) {
        if (acceso.requiereReactivacion())
            return;
        HttpSession sesion = peticion.getSession(true);
        sesion.setAttribute(INICIO_SESION, Instant.now());
        sesion.setAttribute(ID_USUARIO, acceso.id());
    }

    public static void registrarTiempoDeUso(HttpServletRequest peticion, UsuarioBO usuarioBO) throws
        NegocioException {
        HttpSession sesion = peticion.getSession(false);
        if (sesion == null)
            throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
        Integer idUsuario = (Integer) sesion.getAttribute(ID_USUARIO);
        Instant inicioSesion = (Instant) sesion.getAttribute(INICIO_SESION);
        if (idUsuario == null || inicioSesion == null) {
            throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
        }
        Instant finSesion = Instant.now();
        Duration tiempoDeUso = Duration.between(inicioSesion, finSesion);
        usuarioBO.cerrarSesion(idUsuario, tiempoDeUso);
        sesion.setAttribute(INICIO_SESION, finSesion);
    }

    public static void cerrar(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion == null)
            return;
        sesion.invalidate();
    }
}
