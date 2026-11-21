package pe.edu.pucp.bookshop.model.dto;

import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.Libro;

import java.util.List;

// Una página de una vista de resultados, con las opciones de los filtros
public record ResultadoLibros(int total,
                              int pagina,
                              int totalPaginas,
                              List<Libro> libros,
                              List<Entidad> categorias,
                              List<Entidad> autores,
                              List<Entidad> editoriales,
                              List<String> idiomas) {
}
