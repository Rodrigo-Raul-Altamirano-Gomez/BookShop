package me.Business.recomendacion;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import me.Model.entities.catalogo.Libro;

final class SimilitudContenido {
    private SimilitudContenido() { }
    static Map<Integer, Double> puntuar(Libro referencia, List<Libro> libros) {
        Map<Integer, Double> puntos = new HashMap<>();
        Set<String> palabrasReferencia = palabras(referencia.getTitulo() + " " + Objects.toString(referencia.getSinopsis(), ""));
        for (Libro libro : libros) {
            if (libro.getId() == referencia.getId()) continue;
            double valor = 0;
            if (libro.getCategoria().getId() == referencia.getCategoria().getId()) valor += 4;
            if (libro.getAutor().getId() == referencia.getAutor().getId()) valor += 3;
            if (libro.getEditorial().getId() == referencia.getEditorial().getId()) valor += 2;
            if (igual(libro.getProveedor(), referencia.getProveedor())) valor += 1;
            if (igual(libro.getSaga(), referencia.getSaga())) valor += 3;
            Set<String> palabras = palabras(libro.getTitulo() + " " + Objects.toString(libro.getSinopsis(), ""));
            Set<String> union = new HashSet<>(palabrasReferencia);
            union.addAll(palabras);
            palabras.retainAll(palabrasReferencia);
            if (!union.isEmpty()) valor += 3.0 * palabras.size() / union.size();
            double precio = libro.getPrecioBase().doubleValue();
            double base = referencia.getPrecioBase().doubleValue();
            valor += 1.0 - Math.abs(precio - base) / Math.max(1.0, Math.max(precio, base));
            puntos.put(libro.getId(), valor);
        }
        return puntos;
    }
    private static boolean igual(String a, String b) {
        return a != null && !a.isBlank() && b != null && a.strip().equalsIgnoreCase(b.strip());
    }
    private static Set<String> palabras(String texto) {
        String limpio = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        Set<String> palabras = new HashSet<>(Arrays.asList(limpio.split("[^\\p{L}\\p{N}]+")));
        palabras.removeIf(p -> p.length() < 3);
        return palabras;
    }
}
