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
        for (VentaLibroDTO venta : ventas) {
            if (venta.fechaPago().isAfter(ahora.minusDays(90))) {
                cantidades.put(venta.idLibro(), cantidades.getOrDefault(venta.idLibro(), 0L) + venta.cantidad());
            }
        }
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
            // Promedio bayesiano: cada libro parte con 5 reseñas "virtuales" del promedio global.
            double valoracionAjustada = (estrellas + 5 * promedioGlobal) / (cantidadResenas + 5.0);
            // Ventas del libro respecto al más vendido (entre 0 y 1).
            double ventasNormalizadas = 0;
            if (maximo > 0)
                ventasNormalizadas = (double) cantidades.getOrDefault(libro.getId(), 0L) / maximo;
            // Novedad: 1 para un libro de hoy y se reduce a la mitad cada 30 días.
            long dias = ChronoUnit.DAYS.between(libro.getFechaCreacion(), ahora);
            double puntajeNovedad = Math.pow(0.5, dias / 30.0);
            double puntaje = 0.60 * ventasNormalizadas + 0.25 * valoracionAjustada / 5.0 + 0.15 * puntajeNovedad;
            puntos.put(libro.getId(), puntaje);
        }
        return puntos;
    }
}
