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

    private Sesion() { }

    public static int obtenerIdUsuario(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion == null) return 0;
        try {
            Integer idUsuario = (Integer) sesion.getAttribute(ID_USUARIO);
            return idUsuario == null ? 0 : idUsuario;
        } catch (IllegalStateException e) {
            // Otra petición pudo cerrar la sesión mientras se consultaba.
            return 0;
        }
    }

    public static int exigirIdUsuario(HttpServletRequest peticion) {
        int idUsuario = obtenerIdUsuario(peticion);
        if (idUsuario <= 0) throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
        return idUsuario;
    }

    public static void iniciar(HttpServletRequest peticion, AccesoUsuarioDTO acceso) {
        if (acceso.requiereReactivacion()) return;
        HttpSession sesion = peticion.getSession(true);
        peticion.changeSessionId();
        sesion.setAttribute(INICIO_SESION, Instant.now());
        sesion.setAttribute(ID_USUARIO, acceso.id());
    }

    public static void registrarTiempoDeUso(HttpServletRequest peticion, UsuarioBO usuarioBO) throws NegocioException {
        HttpSession sesion = peticion.getSession(false);
        if (sesion == null) throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
        synchronized (sesion) {
            Integer idUsuario;
            Instant inicioSesion;
            try {
                idUsuario = (Integer) sesion.getAttribute(ID_USUARIO);
                inicioSesion = (Instant) sesion.getAttribute(INICIO_SESION);
            } catch (IllegalStateException e) {
                throw new NotAuthorizedException("La sesión ya está cerrada.", "Sesion");
            }
            if (idUsuario == null || inicioSesion == null) {
                throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
            }
            Instant finSesion = Instant.now();
            Duration tiempoDeUso = Duration.between(inicioSesion, finSesion);
            usuarioBO.cerrarSesion(idUsuario, tiempoDeUso);
            try {
                sesion.setAttribute(INICIO_SESION, finSesion);
            } catch (IllegalStateException e) {
                // El tiempo ya se guardó; otra petición cerró la sesión entretanto.
            }
        }
    }

    public static void cerrar(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion == null) return;
        try {
            sesion.invalidate();
        } catch (IllegalStateException e) {
            // Una sesión cerrada por otra petición ya no requiere invalidación.
        }
    }
}
