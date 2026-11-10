package me.App;

import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.ResumenCompraDTO;
import me.Model.dto.catalogo.DetalleLibroDTO;
import me.Business.catalogo.LibroBOImpl;
import me.Business.compra.CarritoBOImpl;
import me.Business.compra.PedidoBOImpl;
import me.Business.exceptions.NegocioException;
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

            System.out.println("\n2. Agregar un ejemplar al carrito");
            System.out.println(carrito.agregar(usuario, libro, 1));

            System.out.println("\n3. Cambiar la cantidad y recalcular el resumen");
            ResumenCompraDTO resumen = carrito.cambiarCantidad(usuario, libro, cantidad);
            System.out.println(resumen);

            System.out.println("\n4. Preparar la compra y seleccionar los datos del cliente");
            pedidos.preparar(usuario);
            PreparacionCompraDTO preparacion = pedidos.seleccionar(
                    usuario, new SeleccionCompraDTO(direccion, metodo, cupon));
            System.out.println(preparacion.resumen());

            System.out.println("\n5. Ejecutar el pago simulado de Business");
            DetallePedidoDTO confirmado = pedidos.pagar(usuario);
            int idPedido = confirmado.pedido().id();
            System.out.println("Pedido registrado: " + idPedido);
            System.out.println("Estado: " + confirmado.pedido().estado());
            System.out.println("Total confirmado: S/ " + confirmado.pedido().total());

            System.out.println("\n6. Consultar el pedido persistido y comprobar el carrito");
            DetallePedidoDTO guardado = pedidos.consultar(usuario, idPedido);
            System.out.println("OK: pedido recuperado, importe conservado y carrito vacío.");
        } catch (NegocioException e) {
            System.out.println(e.getMessage());
        } finally {
            DBManager.close();
        }
    }
}