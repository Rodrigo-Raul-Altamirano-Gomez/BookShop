package me.Business.recomendacion;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.Model.dto.compra.VentaLibroDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;
import me.Model.entities.catalogo.Libro;

final class Popularidad {
    private Popularidad() { }
    static Map<Integer, Double> puntuar(List<Libro> libros, List<VentaLibroDTO> ventas,
            List<ValoracionLibroDTO> valoraciones, LocalDateTime ahora) {
        Map<Integer, Long> cantidades = new HashMap<>();
        for (VentaLibroDTO venta : ventas)
            if (!venta.fechaPago().isBefore(ahora.minusDays(90)) && !venta.fechaPago().isAfter(ahora))
                cantidades.merge(venta.idLibro(), (long) venta.cantidad(), Long::sum);
        Map<Integer, ValoracionLibroDTO> porLibro = new HashMap<>();
        long resenas = 0, suma = 0;
        for (ValoracionLibroDTO v : valoraciones) {
            porLibro.put(v.idLibro(), v);
            resenas += v.cantidadResenas();
            suma += v.sumaPuntuaciones();
        }
        double promedioGlobal = resenas == 0 ? 3.0 : (double) suma / resenas;
        long maximo = cantidades.values().stream().mapToLong(Long::longValue).max().orElse(0);
        Map<Integer, Double> puntos = new HashMap<>();
        for (Libro libro : libros) {
            ValoracionLibroDTO v = porLibro.get(libro.getId());
            long n = v == null ? 0 : v.cantidadResenas();
            long estrellas = v == null ? 0 : v.sumaPuntuaciones();
            double bayesiana = (estrellas + 5 * promedioGlobal) / (n + 5.0);
            double ventasNormalizadas = maximo == 0 ? 0
                    : Math.log1p(cantidades.getOrDefault(libro.getId(), 0L)) / Math.log1p(maximo);
            long dias = Math.max(0, ChronoUnit.DAYS.between(libro.getFechaCreacion(), ahora));
            double recencia = Math.exp(-dias / 30.0);
            puntos.put(libro.getId(), .60 * ventasNormalizadas + .25 * bayesiana / 5.0 + .15 * recencia);
        }
        return puntos;
    }
}
