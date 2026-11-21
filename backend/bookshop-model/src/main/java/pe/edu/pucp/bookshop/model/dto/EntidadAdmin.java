package pe.edu.pucp.bookshop.model.dto;

import pe.edu.pucp.bookshop.model.Entidad;

// Fila de la tabla de administración de autores, categorías y editoriales
public record EntidadAdmin(Entidad entidad, int cantidadLibros) {
}
