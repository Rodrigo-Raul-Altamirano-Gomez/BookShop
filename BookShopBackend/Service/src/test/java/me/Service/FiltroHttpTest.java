package me.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class FiltroHttpTest {
    @Test
    void modificacionSinCabeceraNoLlegaAlRecurso() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain cadena = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        new FiltroHttp().doFilter(request, response, cadena);
        verify(response).setStatus(403);
        verifyNoInteractions(cadena);
    }

    @Test
    void modificacionConCabeceraContinuaSinCache() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain cadena = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getHeader("X-BookShop-Cliente")).thenReturn("web");
        new FiltroHttp().doFilter(request, response, cadena);
        verify(cadena).doFilter(request, response);
        verify(response).setHeader("Cache-Control", "no-store");
    }
}
