package me.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.NotAuthorizedException;
import me.Business.usuario.UsuarioBO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SesionTest {




    @Test
    void registrarTiempoSinSesionNoInvocaNegocio() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        UsuarioBO usuarios = mock(UsuarioBO.class);
        when(request.getSession(false)).thenReturn(null);

        NotAuthorizedException error = assertThrows(NotAuthorizedException.class,
                () -> Sesion.registrarTiempoDeUso(request, usuarios));
        assertEquals(401, error.getResponse().getStatus());
        verifyNoInteractions(usuarios);
    }





    @Test
    void invalidarSinSesionNoProduceError() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        assertDoesNotThrow(() -> Sesion.cerrar(request));
    }



    @Test
    void invalidarRetiraUnaSesionVigente() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession sesion = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(sesion);

        Sesion.cerrar(request);

        verify(sesion).invalidate();
    }
}
