package me.Model.entities.usuario;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {
    @Test
    void tiempoInicialEsCero() {
        Usuario usuario = new Usuario();
        assertEquals(Duration.ZERO, usuario.getTiempoTotal());
    }

    @Test
    void rolAdministradorDependeDelDominioExactoYNoDelNombreDelCorreo() {
        Usuario usuario = new Usuario();
        usuario.setEmail("  persona@ADMIN.BOOKSHOP  ");
        assertTrue(usuario.tieneCorreoAdministrador());
        usuario.asignarRolPorCorreo();
        assertEquals(Rol.ADMIN, usuario.getRol());

        String[] correosClientes = {
                "admin_persona@bookshop", "persona@admin.bookshop.otro", "@admin.bookshop",
                "persona@otro@admin.bookshop", "persona@otro.admin.bookshop"
        };
        for (String correo : correosClientes) {
            usuario.setEmail(correo);
            assertFalse(usuario.tieneCorreoAdministrador());
            usuario.asignarRolPorCorreo();
            assertEquals(Rol.CLIENTE, usuario.getRol());
        }
    }

    @Test
    void contadorDeEntregasNoPuedeSerNegativo() {
        Usuario usuario = new Usuario();
        usuario.registrarPedidoEntregado();
        usuario.registrarPedidoEntregado();
        assertEquals(2, usuario.getCantidadPedidosEntregados());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> usuario.setCantidadPedidosEntregados(-1));
    }
}
