package pe.edu.pucp.bookshop.model.dto;

import pe.edu.pucp.bookshop.model.Resena;

import java.util.List;

// Reseñas del detalle de un libro (UX-UI §8.3): la propia primero y luego de la más reciente a la más antigua.
// El resumen (promedio, cantidad y distribución de 5 a 1 estrellas) considera solo las visibles.
public record ResenasLibro(List<Resena> resenas,
                           double promedio,
                           int cantidad,
                           List<Integer> distribucion,
                           boolean puedeResenar) {
}
