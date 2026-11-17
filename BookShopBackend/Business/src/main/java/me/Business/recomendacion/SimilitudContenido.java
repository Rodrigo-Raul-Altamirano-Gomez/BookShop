package me.Business.recomendacion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.Model.entities.catalogo.Libro;

final class SimilitudContenido {

    private SimilitudContenido() {
    }

    static Map<Integer, Double> puntuar(Libro referencia, List<Libro> libros) {
        Map<Integer, Double> puntos = new HashMap<>();
        Set<String> palabrasReferencia = separarPalabras(referencia.getTitulo() + " " + referencia.getSinopsis());
        for (Libro libro : libros) {
            if (libro.getId() == referencia.getId())
                continue;
            double valor = 0;
            if (libro.getCategoria().getId() == referencia.getCategoria().getId())
                valor += 4;
            if (libro.getAutor().getId() == referencia.getAutor().getId())
                valor += 3;
            if (libro.getEditorial().getId() == referencia.getEditorial().getId())
                valor += 2;
            if (libro.getProveedor().equalsIgnoreCase(referencia.getProveedor()))
                valor += 1;
            if (libro.getSaga() != null && !libro.getSaga().isBlank() &&
                libro.getSaga().equalsIgnoreCase(referencia.getSaga()))
                valor += 3;
            Set<String> palabras = separarPalabras(libro.getTitulo() + " " + libro.getSinopsis());
            Set<String> union = new HashSet<>(palabrasReferencia);
            union.addAll(palabras);
            palabras.retainAll(palabrasReferencia);
            if (!union.isEmpty())
                valor += 3.0 * palabras.size() / union.size();
            double precio = libro.getPrecioBase().doubleValue();
            double base = referencia.getPrecioBase().doubleValue();
            double diferenciaPrecio = Math.abs(precio - base);
            double precioMayor = Math.max(precio, base);
            if (precioMayor < 1)
                precioMayor = 1;
            valor += 1.0 - diferenciaPrecio / precioMayor;
            puntos.put(libro.getId(), valor);
        }
        return puntos;
    }

    private static Set<String> separarPalabras(String texto) {
        Set<String> significativas = new HashSet<>();
        // Separa el texto por espacios y signos; solo cuentan las palabras de tres letras o más.
        for (String palabra : texto.toLowerCase().split("[\\s.,;:!?¡¿()\"]+")) {
            if (palabra.length() >= 3)
                significativas.add(palabra);
        }
        return significativas;
    }
}
