package me.Model.dto.compra;

// Id de cupon cero significa Sin cupon; direccion/metodo cero significan seleccion incompleta.
// Propietario, carrito y preparacion se obtienen del contexto; estos ids no conceden permisos.
public record SeleccionCompraDTO(
        int idDireccionEnvio,
        int idMetodoPago,
        int idCupon
) {
}
