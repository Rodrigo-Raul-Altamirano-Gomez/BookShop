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

    private Popularidad() {
    }

    static Map<Integer, Double> puntuar(List<Libro> libros, List<VentaLibroDTO> ventas,
        List<ValoracionLibroDTO> valoraciones, LocalDateTime ahora) {
        Map<Integer, Long> cantidades = new HashMap<>();
        for (VentaLibroDTO venta : ventas) if (!venta.fechaPago().isBefore(ahora.minusDays(90)) &&
            !venta.fechaPago().isAfter(ahora))
            cantidades.put(venta.idLibro(), cantidades.getOrDefault(venta.idLibro(), 0L) + venta.cantidad());
        Map<Integer, ValoracionLibroDTO> porLibro = new HashMap<>();
        long resenas = 0, suma = 0;
        for (ValoracionLibroDTO valoracion : valoraciones) {
            porLibro.put(valoracion.idLibro(), valoracion);
            resenas += valoracion.cantidadResenas();
            suma += valoracion.sumaPuntuaciones();
        }
        double promedioGlobal = resenas == 0 ? 3.0 : (double) suma / resenas;
        long maximo = 0;
        for (Long cantidad : cantidades.values()) {
            if (cantidad > maximo)
                maximo = cantidad;
        }
        Map<Integer, Double> puntos = new HashMap<>();
        for (Libro libro : libros) {
            ValoracionLibroDTO valoracion = porLibro.get(libro.getId());
            long cantidadResenas = valoracion == null ? 0 : valoracion.cantidadResenas();
            long estrellas = valoracion == null ? 0 : valoracion.sumaPuntuaciones();
            double valoracionAjustada = (estrellas + 5 * promedioGlobal) / (cantidadResenas + 5.0);
            double ventasNormalizadas = maximo == 0 ? 0 : Math.log1p(cantidades.getOrDefault(libro.getId(),
                0L)) / Math.log1p(maximo);
            long dias = Math.max(0, ChronoUnit.DAYS.between(libro.getFechaCreacion(), ahora));
            double puntajeNovedad = Math.exp(-dias / 30.0);
            puntos.put(libro.getId(), .60 * ventasNormalizadas + .25 * valoracionAjustada / 5.0 + .15 *
                puntajeNovedad);
        }
        return puntos;
    }
}
