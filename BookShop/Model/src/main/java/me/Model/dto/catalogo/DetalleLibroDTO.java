package me.Model.dto.catalogo;

import me.Model.dto.opinion.ValoracionLibroDTO;

// Reutilizable en detalle y comparacion. El nombre del autor esta en la tarjeta del libro.
public record DetalleLibroDTO(
        TarjetaLibroDTO libro,
        int idAutor,
        int idCategoria,
        String nombreCategoria,
        int idEditorial,
        String nombreEditorial,
        String idioma,
        String sinopsis,
        String isbn,
        int anioEdicion,
        int numeroPaginas,
        Double peso,
        Double alto,
        Double ancho,
        String saga,
        Integer numeroTomo,
        int stockDisponible,
        ValoracionLibroDTO valoracion,
        boolean favorito,
        boolean deseado
) {
}
