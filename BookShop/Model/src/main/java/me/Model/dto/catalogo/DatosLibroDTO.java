package me.Model.dto.catalogo;

// Campos de Datos del libro, compartidos con el registro. El id se recibe en la operacion de edicion.
public record DatosLibroDTO(
        String titulo,
        String idioma,
        String sinopsis,
        String proveedor,
        String isbn,
        int anioEdicion,
        int numeroPaginas,
        Double peso,
        Double alto,
        Double ancho,
        String saga,
        Integer numeroTomo
) {
}
