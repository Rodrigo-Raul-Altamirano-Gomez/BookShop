package me.Service;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter("/api/*")
public class FiltroHttp implements Filter {
    @Override
    public void doFilter(ServletRequest entrada, ServletResponse salida, FilterChain cadena)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) entrada;
        HttpServletResponse response = (HttpServletResponse) salida;
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        // La cabecera no es una credencial. Impide envíos cruzados simples con cookies.
        String metodo = request.getMethod();
        if (!metodo.equals("GET") && !metodo.equals("HEAD") && !metodo.equals("OPTIONS")
                && !"web".equals(request.getHeader("X-BookShop-Cliente"))) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"codigo\":\"SOLICITUD_NO_PERMITIDA\","
                    + "\"mensaje\":\"Falta la cabecera X-BookShop-Cliente.\",\"campo\":null}");
            return;
        }
        cadena.doFilter(entrada, salida);
    }
}
