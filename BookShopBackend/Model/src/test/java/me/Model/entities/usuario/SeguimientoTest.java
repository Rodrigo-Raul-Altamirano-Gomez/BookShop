package me.Model.entities.usuario;

import java.time.LocalDateTime;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Editorial;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeguimientoTest {
    @Test
    void seguimientosCompartenIdentidadYAuditoriaAlRetirarseYReactivarse() {
        Usuario usuario = new Usuario();
        Autor autor = new Autor();
        Editorial editorial = new Editorial();
        Categoria categoria = new Categoria();
        SeguimientoAutor deAutor = new SeguimientoAutor(1, usuario, autor);
        SeguimientoEditorial deEditorial = new SeguimientoEditorial(2, usuario, editorial);
        SeguimientoCategoria deCategoria = new SeguimientoCategoria(3, usuario, categoria);
        Seguimiento[] seguimientos = {deAutor, deEditorial, deCategoria};
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 5, 12, 0);

        for (int i = 0; i < seguimientos.length; i++) {
            Seguimiento seguimiento = seguimientos[i];
            seguimiento.setUsuarioCreacion("lector@bookshop");
            seguimiento.setFechaCreacion(fecha);
            seguimiento.setEliminado(true);
            assertTrue(seguimiento.isEliminado());
            seguimiento.setEliminado(false);
            seguimiento.setUsuarioActualizacion("lector@bookshop");
            seguimiento.setFechaActualizacion(fecha.plusDays(1));

            assertEquals(i + 1, seguimiento.getId());
            assertSame(usuario, seguimiento.getUsuario());
            assertFalse(seguimiento.isEliminado());
            assertEquals(fecha, seguimiento.getFechaCreacion());
            assertEquals("lector@bookshop", seguimiento.getUsuarioCreacion());
            assertEquals(fecha.plusDays(1), seguimiento.getFechaActualizacion());
            assertEquals("lector@bookshop", seguimiento.getUsuarioActualizacion());
        }
        assertSame(autor, deAutor.getAutor());
        assertSame(editorial, deEditorial.getEditorial());
        assertSame(categoria, deCategoria.getCategoria());
    }
}
