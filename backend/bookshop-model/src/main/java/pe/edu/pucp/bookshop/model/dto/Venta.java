package pe.edu.pucp.bookshop.model.dto;

import pe.edu.pucp.bookshop.model.Libro;

import java.time.LocalDateTime;

// Un detalle de un pedido que cuenta como venta válida (ERS §2.3), con los datos que usan las
// recomendaciones y los reportes. El libro trae su id, título y los nombres de su categoría, autor y editorial.
public record Venta(int idPedido,
                    int idUsuario,
                    LocalDateTime fecha,
                    double totalPedido,
                    Libro libro,
                    int cantidad,
                    double subtotal) {
}
