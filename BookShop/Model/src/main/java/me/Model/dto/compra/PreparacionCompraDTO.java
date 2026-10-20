package me.Model.dto.compra;

// Preparacion en memoria, sin id de pedido ni datos de un pago confirmado.
// Seleccion conserva las elecciones aunque sus tarjetas no esten en la pagina visible.
public record PreparacionCompraDTO(
        ResumenCompraDTO resumen,
        SeleccionCompraDTO seleccion,
        boolean puedePagar
) {
}
