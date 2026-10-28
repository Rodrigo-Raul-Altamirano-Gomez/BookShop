package me.App;

import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.ResumenCompraDTO;
import me.Model.dto.catalogo.DetalleLibroDTO;
import me.Business.catalogo.LibroBOImpl;
import me.Business.compra.CarritoBOImpl;
import me.Business.compra.PedidoBOImpl;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.DBManager.DBManager;
import me.Model.dto.compra.SeleccionCompraDTO;

public class Main {
    public static void main(String[] args) {
        // Ajustar estos IDs a los registros de la base MySQL de desarrollo.
        int usuario = 1;
        int libro = 3;
        int cantidad = 2;
        int direccion = 1;
        int metodo = 1;
        int cupon = 0;

        System.out.println("=== BookShopBackend: prueba del flujo de compra ===");
        System.out.println("El pago simulado registra el pedido y descuenta stock en MySQL de desarrollo.");
        try {
            LibroBOImpl catalogo = new LibroBOImpl();
            CarritoBOImpl carrito = new CarritoBOImpl();
            PedidoBOImpl pedidos = new PedidoBOImpl();

            System.out.println("\n1. Consultar el libro antes de comprar");
            DetalleLibroDTO detalle = catalogo.obtenerDetalle(usuario, libro);
            System.out.println("Libro: " + detalle.libro().titulo());
            System.out.println("Precio vigente: S/ " + detalle.libro().precioConDescuento());
            System.out.println("Stock disponible: " + detalle.stockDisponible());
            if (detalle.stockDisponible() <= 0) {
                throw new IllegalStateException("El escenario requiere un libro con stock.");
            }
            if (carrito.consultar(usuario).cantidadLibros() != 0) {
                throw new IllegalStateException("El carrito inicial debe estar vacío.");
            }

            System.out.println("\n2. Agregar un ejemplar al carrito");
            System.out.println(carrito.agregar(usuario, libro, 1));
            if (carrito.consultar(usuario).cantidadLibros() != 1) {
                throw new IllegalStateException("No se agregó el ejemplar.");
            }

            System.out.println("\n3. Cambiar la cantidad y recalcular el resumen");
            ResumenCompraDTO resumen = carrito.cambiarCantidad(usuario, libro, cantidad);
            System.out.println(resumen);
            if (resumen.cantidadLibros() != cantidad) {
                throw new IllegalStateException("La cantidad del carrito no coincide.");
            }

            System.out.println("\n4. Intentar una cantidad inválida y verificar que no altera el carrito");
            boolean rechazado = false;
            try {
                carrito.cambiarCantidad(usuario, libro, -1);
            } catch (ValidacionException e) {
                rechazado = true;
                System.out.println("Rechazo esperado: " + e.getMessage());
            }
            if (!rechazado) {
                throw new IllegalStateException("Business aceptó una cantidad negativa.");
            }
            if (!carrito.consultar(usuario).equals(resumen)) {
                throw new IllegalStateException("El rechazo modificó el carrito.");
            }

            System.out.println("\n5. Preparar la compra y seleccionar los datos del cliente");
            pedidos.preparar(usuario);
            PreparacionCompraDTO preparacion = pedidos.seleccionar(
                    usuario, new SeleccionCompraDTO(direccion, metodo, cupon));
            System.out.println(preparacion.resumen());
            if (!preparacion.puedePagar()) {
                throw new IllegalStateException("La selección no permite pagar.");
            }

            System.out.println("\n6. Ejecutar el pago simulado de Business");
            DetallePedidoDTO confirmado = pedidos.pagar(usuario);
            int idPedido = confirmado.pedido().id();
            if (idPedido <= 0) {
                throw new IllegalStateException("El pago no devolvió un ID de pedido válido.");
            }
            System.out.println("Pedido registrado: " + idPedido);
            System.out.println("Estado: " + confirmado.pedido().estado());
            System.out.println("Total confirmado: S/ " + confirmado.pedido().total());

            System.out.println("\n7. Consultar el pedido persistido y comprobar el carrito");
            DetallePedidoDTO guardado = pedidos.consultar(usuario, idPedido);
            if (guardado.pedido().id() != idPedido) {
                throw new IllegalStateException("La consulta devolvió otro pedido.");
            }
            if (guardado.resumen().cantidadLibros() != cantidad) {
                throw new IllegalStateException("No coinciden los ejemplares del pedido.");
            }
            if (guardado.pedido().total().compareTo(confirmado.pedido().total()) != 0) {
                throw new IllegalStateException("El importe consultado difiere del importe confirmado.");
            }
            if (carrito.consultar(usuario).cantidadLibros() != 0) {
                throw new IllegalStateException("El carrito no se vació después del pago.");
            }
            System.out.println("OK: pedido recuperado, importe conservado y carrito vacío.");
        } catch (NegocioException e) {
            throw new IllegalStateException("Business detuvo la prueba: " + e.getMessage(), e);
        } finally {
            DBManager.close();
        }
    }
}