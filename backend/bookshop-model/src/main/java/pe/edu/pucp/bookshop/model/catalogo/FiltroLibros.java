package pe.edu.pucp.bookshop.model.catalogo;

import java.time.LocalDate;
import java.util.List;

// Búsqueda, filtros, orden y página de una vista de resultados de libros (ERS RF08)
public record FiltroLibros(String q,
                           List<Integer> categorias,
                           List<Integer> autores,
                           List<Integer> editoriales,
                           String idioma,
                           Double min,
                           Double max,
                           LocalDate desde,
                           LocalDate hasta,
                           String orden,
                           int pagina,
                           int tamano) {
}
