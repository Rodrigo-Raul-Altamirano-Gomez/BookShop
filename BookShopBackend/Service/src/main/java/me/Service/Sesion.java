package me.Service;

import java.time.Duration;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.NotAuthorizedException;
import me.Business.exceptions.NegocioException;
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

    public static int exigirIdUsuario(HttpServletRequest peticion) {
        int idUsuario = obtenerIdUsuario(peticion);
        if (idUsuario <= 0)
            throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
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
        int idUsuario = exigirIdUsuario(peticion);
        HttpSession sesion = peticion.getSession(false);
        Instant inicioSesion = (Instant) sesion.getAttribute(INICIO_SESION);
        Duration tiempoDeUso = Duration.between(inicioSesion, Instant.now());
        usuarioBO.cerrarSesion(idUsuario, tiempoDeUso);
    }

    public static void cerrar(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion == null)
            return;
        sesion.invalidate();
    }
}
