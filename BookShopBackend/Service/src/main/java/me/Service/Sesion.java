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
    private static final String ACTOR = "bookshop.actor";
    private static final String INICIO = "bookshop.inicio";

    private Sesion() { }

    public static int actor(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return 0;
        try {
            Object id = sesion.getAttribute(ACTOR);
            return id instanceof Integer valor && valor > 0 ? valor : 0;
        } catch (IllegalStateException e) {
            // Otra petición pudo cerrar la sesión mientras se consultaba.
            return 0;
        }
    }

    public static int exigir(HttpServletRequest request) {
        int id = actor(request);
        if (id == 0) throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
        return id;
    }

    public static void abrir(HttpServletRequest request, AccesoUsuarioDTO acceso) {
        if (acceso.requiereReactivacion()) return;
        HttpSession sesion = request.getSession(true);
        request.changeSessionId();
        sesion.setAttribute(INICIO, Instant.now());
        sesion.setAttribute(ACTOR, acceso.id());
    }

    public static void registrarTiempo(HttpServletRequest request, UsuarioBO usuarios) throws NegocioException {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
        synchronized (sesion) {
            Object id;
            Object comienzo;
            try {
                id = sesion.getAttribute(ACTOR);
                comienzo = sesion.getAttribute(INICIO);
            } catch (IllegalStateException e) {
                throw new NotAuthorizedException("La sesión ya está cerrada.", "Sesion");
            }
            if (!(id instanceof Integer actor) || actor <= 0 || !(comienzo instanceof Instant inicio)) {
                throw new NotAuthorizedException("Se requiere iniciar sesión.", "Sesion");
            }
            Instant ahora = Instant.now();
            usuarios.cerrarSesion(actor, Duration.between(inicio, ahora));
            try {
                sesion.setAttribute(INICIO, ahora);
            } catch (IllegalStateException e) {
                // El tiempo ya se guardó; otra petición cerró la sesión entretanto.
            }
        }
    }

    public static void invalidar(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return;
        try {
            sesion.invalidate();
        } catch (IllegalStateException e) {
            // Una sesión cerrada por otra petición ya no requiere invalidación.
        }
    }
}
