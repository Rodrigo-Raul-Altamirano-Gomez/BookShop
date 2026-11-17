package me.Business.compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.Business.configuracion.ConfiguracionBO;
import me.Business.configuracion.ConfiguracionBOImpl;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.DescuentoDAO;
import me.DAO.catalogo.DescuentoDAOImpl;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.Model.dto.compra.ItemDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.ResumenCompraDTO;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Libro;

public class CarritoBOImpl implements CarritoBO {

    // Los carritos viven en memoria y son compartidos por todas las instancias (uno por usuario).
    private static final Map<Integer, EstadoCarrito> carritos = new HashMap<>();
    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final DescuentoDAO descuentoDAO = new DescuentoDAOImpl();
    private final ConfiguracionBO configuracionBO = new ConfiguracionBOImpl();

    // Pedido utiliza los mismos datos del carrito para preparar y confirmar la compra.
    static class EstadoCarrito {

        final Map<Integer, ItemDTO> lineas = new LinkedHashMap<>();

        PreparacionCompraDTO preparacion;

        int pedidoConfirmado;
    }

    EstadoCarrito obtenerCarrito(int idUsuario) {
        synchronized (carritos) {
            EstadoCarrito carrito = carritos.get(idUsuario);
            if (carrito == null) {
                carrito = new EstadoCarrito();
                carritos.put(idUsuario, carrito);
            }
            return carrito;
        }
    }

    @Override
    public ResumenCompraDTO consultar(int idUsuario) throws NegocioException {
        EstadoCarrito carrito = obtenerCarrito(idUsuario);
        synchronized (carrito) {
            return consultarResumen(carrito);
        }
    }

    @Override
    public ResumenCompraDTO agregar(int idUsuario, int idLibro, int cantidad) throws NegocioException {
        if (cantidad <= 0)
            throw new ValidacionException("cantidad", "La cantidad debe ser positiva.");
        EstadoCarrito carrito = obtenerCarrito(idUsuario);
        synchronized (carrito) {
            ItemDTO anterior = carrito.lineas.get(idLibro);
            int cantidadTotal = cantidad;
            if (anterior != null)
                cantidadTotal += anterior.cantidad();
            return guardarCantidad(carrito, idLibro, cantidadTotal);
        }
    }

    @Override
    public ResumenCompraDTO cambiarCantidad(int idUsuario, int idLibro, int cantidad) throws NegocioException {
        if (cantidad < 0)
            throw new ValidacionException("cantidad", "La cantidad no puede ser negativa.");
        EstadoCarrito carrito = obtenerCarrito(idUsuario);
        synchronized (carrito) {
            if (!carrito.lineas.containsKey(idLibro))
                throw new RecursoNoEncontradoException("El libro no esta en el carrito.");
            if (cantidad == 0) {
                carrito.lineas.remove(idLibro);
                carrito.preparacion = null;
                carrito.pedidoConfirmado = 0;
                return consultarResumen(carrito);
            }
            return guardarCantidad(carrito, idLibro, cantidad);
        }
    }

    // Agregar y cambiar cantidad validan y recalculan antes de modificar el carrito.
    private ResumenCompraDTO guardarCantidad(EstadoCarrito carrito, int idLibro, int cantidad)
        throws NegocioException {
        ItemDTO anterior = carrito.lineas.get(idLibro);
        ItemDTO linea = consultarLinea(idLibro, cantidad, anterior);
        if (!linea.activo())
            throw new NegocioException("El libro no esta disponible.");
        if (cantidad > linea.stockDisponible())
            throw new NegocioException("No hay stock suficiente de " + linea.titulo() + ".");
        List<ItemDTO> items = new ArrayList<>();
        for (ItemDTO existente : carrito.lineas.values()) {
            if (existente.idLibro() == idLibro)
                items.add(linea);
            else
                items.add(consultarLinea(existente.idLibro(), existente.cantidad(), existente));
        }
        if (anterior == null)
            items.add(linea);
        ResumenCompraDTO resumen = calcularResumen(items, configuracionBO.obtener().porcentajeEnvio(), 0,
            BigDecimal.ZERO);
        carrito.lineas.put(idLibro, linea);
        carrito.preparacion = null;
        carrito.pedidoConfirmado = 0;
        return resumen;
    }

    @Override
    public ResumenCompraDTO retirar(int idUsuario, int idLibro) throws NegocioException {
        return cambiarCantidad(idUsuario, idLibro, 0);
    }

    @Override
    public ResumenCompraDTO vaciar(int idUsuario) throws NegocioException {
        EstadoCarrito carrito = obtenerCarrito(idUsuario);
        synchronized (carrito) {
            carrito.lineas.clear();
            carrito.preparacion = null;
            carrito.pedidoConfirmado = 0;
            return consultarResumen(carrito);
        }
    }

    ResumenCompraDTO consultarResumen(EstadoCarrito carrito) throws NegocioException {
        List<ItemDTO> items = new ArrayList<>();
        for (ItemDTO linea : carrito.lineas.values()) {
            items.add(consultarLinea(linea.idLibro(), linea.cantidad(), linea));
        }
        return calcularResumen(items, configuracionBO.obtener().porcentajeEnvio(), 0, BigDecimal.ZERO);
    }

    // Actualiza precio y disponibilidad; conserva visible una línea cuyo libro fue retirado.
    private ItemDTO consultarLinea(int idLibro, int cantidad, ItemDTO anterior) throws NegocioException {
        try {
            Libro libro = libroDAO.leer(idLibro);
            if (libro == null) {
                if (anterior == null)
                    throw new RecursoNoEncontradoException("El libro no existe.");
                return new ItemDTO(idLibro, anterior.titulo(), anterior.portada(), cantidad,
                    anterior.precioOriginal(), anterior.precioUnitario(), anterior.porcentajeDescuento(),
                    anterior.precioUnitario().multiply(BigDecimal.valueOf(cantidad)), false, 0);
            }
            Descuento descuento = descuentoDAO.obtenerVigente(idLibro, LocalDateTime.now());
            BigDecimal porcentaje = descuento == null ? BigDecimal.ZERO : descuento.getPorcentaje();
            BigDecimal precioFinal =
                libro.getPrecioBase().multiply(BigDecimal.valueOf(100).subtract(porcentaje)).divide(
                BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return new ItemDTO(idLibro, libro.getTitulo(), libro.getPortada(), cantidad,
                libro.getPrecioBase(), precioFinal, porcentaje,
                precioFinal.multiply(BigDecimal.valueOf(cantidad)), libro.isActivo(), libro.getStockFisico());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el carrito.", e);
        }
    }

    // Carrito y Pedido utilizan el mismo cálculo de descuentos, envío y total.
    static ResumenCompraDTO calcularResumen(List<ItemDTO> items, BigDecimal porcentajeEnvio, int idCupon,
        BigDecimal porcentajeCupon) {
        BigDecimal productos = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        int cantidad = 0;
        for (ItemDTO item : items) {
            productos = productos.add(item.precioOriginal().multiply(BigDecimal.valueOf(item.cantidad())));
            subtotal = subtotal.add(item.subtotal());
            cantidad += item.cantidad();
        }
        BigDecimal descuentoCupon = subtotal.multiply(porcentajeCupon).divide(BigDecimal.valueOf(100), 2,
            RoundingMode.HALF_UP);
        BigDecimal envio = subtotal.multiply(porcentajeEnvio).divide(BigDecimal.valueOf(100), 2,
            RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(descuentoCupon).add(envio);
        return new ResumenCompraDTO(items, productos.setScale(2), productos.subtract(subtotal).setScale(2),
            idCupon, porcentajeCupon, descuentoCupon, porcentajeEnvio, envio, total, cantidad);
    }
}
