package pe.edu.pucp.bookshop.model.dto;

import pe.edu.pucp.bookshop.model.Cupon;

import java.util.List;

// Importes del carrito y del resumen de compra (ERS RF11.4–5 y RF15.3). cupon es null si no se aplica ninguno.
public record ResumenCompra(List<LineaCompra> lineas,
                            Cupon cupon,
                            double subtotal,
                            double descuentoCupon,
                            double costoEnvio,
                            double total) {
}
