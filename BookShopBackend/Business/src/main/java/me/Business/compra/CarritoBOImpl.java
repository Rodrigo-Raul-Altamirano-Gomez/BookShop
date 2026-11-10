package me.Business.compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.Business.Autorizacion;
import me.Business.configuracion.ConfiguracionBOImpl;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.StockInsuficienteException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.DescuentoDAO;
import me.DAO.catalogo.DescuentoDAOImpl;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.compra.ItemDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.ResumenCompraDTO;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Libro;

public class CarritoBOImpl implements CarritoBO {
    private static final Map<Integer, EstadoCarrito> CARRITOS = new HashMap<>();
    private final Map<Integer, EstadoCarrito> carritos;
    private final LibroDAO libroDAO;
    private final DescuentoDAO descuentoDAO;
    private final Autorizacion autorizacion;
    private final Clock reloj;
    private final ProveedorConfiguracion configuracion;

    public CarritoBOImpl() {
        this(new LibroDAOImpl(), new DescuentoDAOImpl(), new UsuarioDAOImpl(),
            Clock.system(ZoneId.of("America/Lima")), ConfiguracionBOImpl::actual, CARRITOS);
    }

    public CarritoBOImpl(LibroDAO libroDAO, DescuentoDAO descuentoDAO, UsuarioDAO usuarioDAO,
        Clock reloj, ProveedorConfiguracion configuracion) {
        this(libroDAO, descuentoDAO, usuarioDAO, reloj, configuracion, new HashMap<>());
    }

    private CarritoBOImpl(LibroDAO libroDAO, DescuentoDAO descuentoDAO, UsuarioDAO usuarioDAO,
        Clock reloj, ProveedorConfiguracion configuracion, Map<Integer, EstadoCarrito> carritos) {
        this.libroDAO = libroDAO;
        this.descuentoDAO = descuentoDAO;
        this.autorizacion = new Autorizacion(usuarioDAO);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
        this.configuracion = configuracion;
        this.carritos = carritos;
    }

    // Pedido utiliza el mismo carrito. El bloqueo evita modificarlo mientras se paga.
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
        autorizacion.usuarioActivo(idUsuario);
        EstadoCarrito carrito = obtenerCarrito(idUsuario);
        synchronized (carrito) {
            return consultarResumen(carrito);
        }
    }

    @Override
    public ResumenCompraDTO agregar(int idUsuario, int idLibro, int cantidad) throws NegocioException {
        autorizacion.usuarioActivo(idUsuario);
        if (cantidad <= 0) throw new ValidacionException("cantidad", "La cantidad debe ser positiva.");

        EstadoCarrito carrito = obtenerCarrito(idUsuario);
        synchronized (carrito) {
            try {
                Libro libro = libroDAO.leer(idLibro);
                if (libro == null) throw new RecursoNoEncontradoException("El libro no existe.");
                if (!libro.isActivo()) throw new ConflictoException("El libro no esta disponible.");
                ItemDTO anterior = carrito.lineas.get(idLibro);
                long cantidadTotal = cantidad;
                if (anterior != null) cantidadTotal += anterior.cantidad();
                if (cantidadTotal > libro.getStockFisico()) {
                    throw new StockInsuficienteException("No hay stock suficiente de " + libro.getTitulo() + ".");
                }

                Descuento descuento = descuentoDAO.obtenerVigente(idLibro, LocalDateTime.now(reloj));
                BigDecimal porcentaje = descuento == null ? BigDecimal.ZERO : descuento.getPorcentaje();
                BigDecimal precioFinal = libro.getPrecioBase().multiply(BigDecimal.valueOf(100).subtract(porcentaje))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                ItemDTO linea = new ItemDTO(idLibro, libro.getTitulo(),
                    libro.getPortada(), (int) cantidadTotal,
                    libro.getPrecioBase(), precioFinal, porcentaje,
                    precioFinal.multiply(BigDecimal.valueOf(cantidadTotal)), true, libro.getStockFisico());
                List<ItemDTO> items = new ArrayList<>();
                boolean reemplazada = false;
                for (ItemDTO existente : carrito.lineas.values()) {
                    if (existente.idLibro() == idLibro) {
                        items.add(linea);
                        reemplazada = true;
                    } else {
                        items.add(consultarLinea(existente.idLibro(), existente.cantidad(), existente));
                    }
                }
                if (!reemplazada) items.add(linea);
                ResumenCompraDTO resumen = calcularResumen(items, configuracion.obtener().porcentajeEnvio(), 0, BigDecimal.ZERO);
                carrito.lineas.put(idLibro, linea);
                carrito.preparacion = null;
                carrito.pedidoConfirmado = 0;
                return resumen;
            } catch (SQLException e) {
                throw new PersistenciaException("No se pudo agregar el libro al carrito.", e);
            }
        }
    }

    @Override
    public ResumenCompraDTO cambiarCantidad(int idUsuario, int idLibro, int cantidad) throws NegocioException {
        autorizacion.usuarioActivo(idUsuario);
        if (cantidad < 0) throw new ValidacionException("cantidad", "La cantidad no puede ser negativa.");

        EstadoCarrito carrito = obtenerCarrito(idUsuario);
        synchronized (carrito) {
            if (!carrito.lineas.containsKey(idLibro)) throw new RecursoNoEncontradoException("El libro no esta en el carrito.");
            if (cantidad == 0) {
                carrito.lineas.remove(idLibro);
                carrito.preparacion = null;
                carrito.pedidoConfirmado = 0;
                return consultarResumen(carrito);
            }
            ItemDTO linea = consultarLinea(idLibro, cantidad, carrito.lineas.get(idLibro));
            if (!Boolean.TRUE.equals(linea.activo())) throw new ConflictoException("El libro no esta disponible.");
            if (cantidad > linea.stockDisponible()) {
                throw new StockInsuficienteException("No hay stock suficiente de " + linea.titulo() + ".");
            }
            List<ItemDTO> items = new ArrayList<>();
            for (ItemDTO existente : carrito.lineas.values()) {
                if (existente.idLibro() == idLibro) items.add(linea);
                else items.add(consultarLinea(existente.idLibro(), existente.cantidad(), existente));
            }
            ResumenCompraDTO resumen = calcularResumen(items, configuracion.obtener().porcentajeEnvio(), 0, BigDecimal.ZERO);
            carrito.lineas.put(idLibro, linea);
            carrito.preparacion = null;
            carrito.pedidoConfirmado = 0;
            return resumen;
        }
    }

    @Override
    public ResumenCompraDTO retirar(int idUsuario, int idLibro) throws NegocioException {
        return cambiarCantidad(idUsuario, idLibro, 0);
    }

    @Override
    public ResumenCompraDTO vaciar(int idUsuario) throws NegocioException {
        autorizacion.usuarioActivo(idUsuario);
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
        return calcularResumen(items, configuracion.obtener().porcentajeEnvio(), 0, BigDecimal.ZERO);
    }

    // Actualiza precio y disponibilidad; conserva visible una línea cuyo libro fue retirado.
    private ItemDTO consultarLinea(int idLibro, int cantidad, ItemDTO anterior) throws NegocioException {
        try {
            Libro libro = libroDAO.leer(idLibro);
            if (libro == null) {
                return new ItemDTO(idLibro, anterior.titulo(), anterior.portada(), cantidad, anterior.precioOriginal(),
                    anterior.precioUnitario(), anterior.porcentajeDescuento(),
                    anterior.precioUnitario().multiply(BigDecimal.valueOf(cantidad)), false, 0);
            }
            Descuento descuento = descuentoDAO.obtenerVigente(idLibro, LocalDateTime.now(reloj));
            BigDecimal porcentaje = descuento == null ? BigDecimal.ZERO : descuento.getPorcentaje();
            BigDecimal precioFinal = libro.getPrecioBase().multiply(BigDecimal.valueOf(100).subtract(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return new ItemDTO(idLibro, libro.getTitulo(), libro.getPortada(), cantidad,
                libro.getPrecioBase(), precioFinal, porcentaje, precioFinal.multiply(BigDecimal.valueOf(cantidad)),
                libro.isActivo(), libro.getStockFisico());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el carrito.", e);
        }
    }

    // Carrito y Pedido utilizan el mismo cálculo de descuentos, envío y total.
    static ResumenCompraDTO calcularResumen(List<ItemDTO> items, BigDecimal porcentajeEnvio,
        int idCupon, BigDecimal porcentajeCupon) throws ValidacionException {
        BigDecimal productos = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        long cantidad = 0;
        for (ItemDTO item : items) {
            productos = productos.add(item.precioOriginal().multiply(BigDecimal.valueOf(item.cantidad())));
            subtotal = subtotal.add(item.subtotal());
            cantidad += item.cantidad();
        }
        if (cantidad > Integer.MAX_VALUE) throw new ValidacionException("cantidad", "El carrito contiene demasiados ejemplares.");
        BigDecimal descuentoCupon = subtotal.multiply(porcentajeCupon)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal envio = subtotal.multiply(porcentajeEnvio)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(descuentoCupon).add(envio);
        if (productos.compareTo(new BigDecimal("999999999999.99")) > 0
            || total.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new ValidacionException("importe", "El importe excede el limite permitido por pedido. Reduce la cantidad.");
        }
        return new ResumenCompraDTO(items, productos.setScale(2), productos.subtract(subtotal).setScale(2), idCupon, porcentajeCupon,
            descuentoCupon, porcentajeEnvio, envio, total, (int) cantidad);
    }
}
