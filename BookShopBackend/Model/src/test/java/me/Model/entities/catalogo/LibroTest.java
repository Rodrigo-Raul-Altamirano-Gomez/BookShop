package me.Model.entities.catalogo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LibroTest {
    @Test
    void vetosDirectosNoSeDuplicanNiRetiranVetosDeReferencias() {
        Libro libro = new Libro();
        libro.setNumeroVetos(2);
        libro.aplicarVetoDirecto();
        libro.aplicarVetoDirecto();
        assertEquals(3, libro.getNumeroVetos());
        assertFalse(libro.isActivo());

        libro.retirarVetoDirecto();
        libro.retirarVetoDirecto();
        assertEquals(2, libro.getNumeroVetos());
        assertFalse(libro.isActivo());
        libro.setActivo(true);
        assertEquals(2, libro.getNumeroVetos());
    }

    @Test
    void recalculoConsideraLosTresVetosHeredadosYElDirecto() {
        Autor autor = new Autor(1, "Autor", "Biografia");
        Editorial editorial = new Editorial(2, "Editorial", "Introduccion");
        Categoria categoria = new Categoria(3, "Categoria", "Introduccion");
        autor.setActivo(false);
        editorial.setEliminado(true);
        categoria.setActivo(false);

        Libro libro = new Libro();
        libro.setAutor(autor);
        libro.setEditorial(editorial);
        libro.setCategoria(categoria);
        libro.recalcularVetos();
        libro.aplicarVetoDirecto();
        assertEquals(4, libro.getNumeroVetos());

        autor.setActivo(true);
        libro.recalcularVetos();
        assertEquals(3, libro.getNumeroVetos());
        assertFalse(libro.isActivo());
        editorial.setEliminado(false);
        categoria.setActivo(true);
        libro.retirarVetoDirecto();
        libro.recalcularVetos();
        assertEquals(0, libro.getNumeroVetos());
        assertTrue(libro.isActivo());
    }

    @Test
    void noSeSustituyenReferenciasPorOtrosIdentificadores() {
        Libro libro = new Libro();
        libro.setAutor(new Autor(1, "A", ""));
        libro.setEditorial(new Editorial(1, "E", ""));
        libro.setCategoria(new Categoria(1, "C", ""));
        assertThrows(IllegalStateException.class, () -> libro.setAutor(new Autor(2, "B", "")));
        assertThrows(IllegalStateException.class, () -> libro.setEditorial(new Editorial(2, "F", "")));
        assertThrows(IllegalStateException.class, () -> libro.setCategoria(new Categoria(2, "D", "")));
        assertThrows(IllegalStateException.class, () -> libro.setAutor(null));
        assertDoesNotThrow(() -> libro.setAutor(new Autor(1, "A actualizado", "")));
    }

    @Test
    void stockYContadorNoPuedenVolverseNegativos() {
        Libro libro = new Libro();
        libro.setStockFisico(3);
        assertThrows(IllegalArgumentException.class, () -> libro.descontarStock(4));
        assertEquals(3, libro.getStockFisico());
        libro.descontarStock(2);
        assertEquals(1, libro.getStockFisico());
        libro.reponerStock(2);
        assertEquals(3, libro.getStockFisico());
        assertThrows(IllegalArgumentException.class, () -> libro.setNumeroVetos(-1));
        assertThrows(IllegalArgumentException.class, () -> libro.setNumeroVetos(5));
        libro.setEliminado(true);
        assertFalse(libro.tieneStockSuficiente(1));
    }
}
