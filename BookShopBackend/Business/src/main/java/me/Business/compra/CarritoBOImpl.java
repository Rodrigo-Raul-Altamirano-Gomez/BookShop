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
import me.Business.configuracion.ConfiguracionBOImpl;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.StockInsuficienteException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.DAO.catalogo.DescuentoDAO;
import me.DAO.catalogo.DescuentoDAOImpl;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.ImagenDTO;
import me.Model.dto.compra.ItemDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.ResumenCompraDTO;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Libro;

public class CarritoBOImpl implements CarritoBO {
    private static final Map<Integer, EstadoCarrito> MEMORIA = new HashMap<>();
    private static final BigDecimal MAXIMO_PRECIO = new BigDecimal("9999999999.99");
    private static final BigDecimal MAXIMO_IMPORTE = new BigDecimal("999999999999.99");
    private final Map<Integer, EstadoCarrito> carritos;
    private final LibroDAO libros;
    private final DescuentoDAO descuentos;
    private final Autorizacion autorizacion;
    private final Clock reloj;
    private final ProveedorConfiguracion configuracion;

    public CarritoBOImpl() {
        this(new LibroDAOImpl(), new DescuentoDAOImpl(), new UsuarioDAOImpl(),
                Clock.system(ZoneId.of("America/Lima")), ConfiguracionBOImpl::actual, MEMORIA);
    }

    public CarritoBOImpl(LibroDAO libros, DescuentoDAO descuentos, UsuarioDAO usuarios,
            Clock reloj, ProveedorConfiguracion configuracion) {
        this(libros, descuentos, usuarios, reloj, configuracion, new HashMap<>());
    }

    private CarritoBOImpl(LibroDAO libros, DescuentoDAO descuentos, UsuarioDAO usuarios,
            Clock reloj, ProveedorConfiguracion configuracion, Map<Integer, EstadoCarrito> carritos) {
        this.libros = libros;
        this.descuentos = descuentos;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
        this.configuracion = configuracion;
        this.carritos = carritos;
    }

    // Carrito y Pedido comparten este estado por actor. Su monitor impide pagos repetidos.
    static class EstadoCarrito {
        final Map<Integer, ItemDTO> lineas = new LinkedHashMap<>();
        PreparacionCompraDTO preparacion;
        int pedidoConfirmado;
    }

    EstadoCarrito estado(int idActor) {
        synchronized (carritos) {
            return carritos.computeIfAbsent(idActor, id -> new EstadoCarrito());
        }
    }

    @Override
    public ResumenCompraDTO consultar(int idActor) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        EstadoCarrito estado = estado(idActor);
        synchronized (estado) { return resumen(estado); }
    }

    @Override
    public ResumenCompraDTO agregar(int idActor, int idLibro, int cantidad) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        if (cantidad <= 0) throw new ValidacionException("cantidad", "La cantidad debe ser positiva.");
        EstadoCarrito estado = estado(idActor);
        synchronized (estado) {
            ItemDTO anterior = estado.lineas.get(idLibro);
            int total;
            try { total = Math.addExact(cantidad, anterior == null ? 0 : anterior.cantidad()); }
            catch (ArithmeticException e) { throw new ValidacionException("cantidad", "La cantidad es demasiado grande."); }
            validarCantidadTotal(estado, idLibro, total);
            ItemDTO item = consultarLinea(idLibro, total, anterior);
            validarDisponible(item);
            ResumenCompraDTO resumen = resumenConCambio(estado, idLibro, item);
            validarImportes(resumen);
            estado.lineas.put(idLibro, item);
            invalidarPreparacion(estado);
            return resumen;
        }
    }

    @Override
    public ResumenCompraDTO cambiarCantidad(int idActor, int idLibro, int cantidad) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        if (cantidad < 0) throw new ValidacionException("cantidad", "La cantidad no puede ser negativa.");
        EstadoCarrito estado = estado(idActor);
        synchronized (estado) {
            ItemDTO anterior = estado.lineas.get(idLibro);
            if (anterior == null) throw new RecursoNoEncontradoException("El libro no esta en el carrito.");
            if (cantidad == 0) estado.lineas.remove(idLibro);
            else {
                validarCantidadTotal(estado, idLibro, cantidad);
                ItemDTO item = consultarLinea(idLibro, cantidad, anterior);
                validarDisponible(item);
                ResumenCompraDTO resumen = resumenConCambio(estado, idLibro, item);
                validarImportes(resumen);
                estado.lineas.put(idLibro, item);
                invalidarPreparacion(estado);
                return resumen;
            }
            invalidarPreparacion(estado);
            return resumen(estado);
        }
    }

    @Override
    public ResumenCompraDTO retirar(int idActor, int idLibro) throws NegocioException {
        return cambiarCantidad(idActor, idLibro, 0);
    }

    @Override
    public ResumenCompraDTO vaciar(int idActor) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        EstadoCarrito estado = estado(idActor);
        synchronized (estado) {
            estado.lineas.clear();
            invalidarPreparacion(estado);
            return resumen(estado);
        }
    }

    private void invalidarPreparacion(EstadoCarrito estado) {
        estado.preparacion = null;
        estado.pedidoConfirmado = 0;
    }

    private void validarCantidadTotal(EstadoCarrito estado, int idLibro, int nuevaCantidad) throws ValidacionException {
        long cantidadTotal = nuevaCantidad;
        for (ItemDTO item : estado.lineas.values()) if (item.idLibro() != idLibro) cantidadTotal += item.cantidad();
        if (cantidadTotal > Integer.MAX_VALUE) throw new ValidacionException("cantidad", "El carrito contiene demasiados ejemplares.");
    }

    ResumenCompraDTO resumen(EstadoCarrito estado) throws NegocioException {
        List<ItemDTO> items = new ArrayList<>();
        for (ItemDTO anterior : estado.lineas.values()) items.add(consultarLinea(anterior.idLibro(), anterior.cantidad(), anterior));
        return resumir(items, configuracion.obtener().porcentajeEnvio(), 0, BigDecimal.ZERO);
    }

    private ResumenCompraDTO resumenConCambio(EstadoCarrito estado, int idLibro, ItemDTO reemplazo) throws NegocioException {
        List<ItemDTO> items = new ArrayList<>();
        boolean existente = false;
        for (ItemDTO anterior : estado.lineas.values()) {
            if (anterior.idLibro() == idLibro) { items.add(reemplazo); existente = true; }
            else items.add(consultarLinea(anterior.idLibro(), anterior.cantidad(), anterior));
        }
        if (!existente) items.add(reemplazo);
        return resumir(items, configuracion.obtener().porcentajeEnvio(), 0, BigDecimal.ZERO);
    }

    ItemDTO consultarLinea(int idLibro, int cantidad, ItemDTO anterior) throws NegocioException {
        try {
            Libro libro = libros.leer(idLibro);
            if (libro == null) {
                if (anterior == null) throw new RecursoNoEncontradoException("El libro no existe.");
                return new ItemDTO(idLibro, anterior.titulo(), anterior.portada(), cantidad, anterior.precioOriginal(),
                        anterior.precioUnitario(), anterior.porcentajeDescuento(), anterior.precioUnitario().multiply(BigDecimal.valueOf(cantidad)), false, 0);
            }
            Descuento descuento = descuentos.obtenerVigente(idLibro, LocalDateTime.now(reloj));
            BigDecimal porcentaje = descuento == null ? BigDecimal.ZERO : descuento.getPorcentaje();
            BigDecimal unitario = libro.getPrecioBase().multiply(BigDecimal.valueOf(100).subtract(porcentaje))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return new ItemDTO(idLibro, libro.getTitulo(), new ImagenDTO(libro.getPortada(), libro.getTipoPortada()), cantidad,
                    libro.getPrecioBase(), unitario, porcentaje, unitario.multiply(BigDecimal.valueOf(cantidad)), libro.isActivo(), libro.getStockFisico());
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar el carrito.", e); }
    }

    static void validarDisponible(ItemDTO item) throws NegocioException {
        if (!Boolean.TRUE.equals(item.activo())) throw new ConflictoException("El libro " + item.titulo() + " no esta disponible.");
        if (item.stockDisponible() == null || item.cantidad() > item.stockDisponible())
            throw new StockInsuficienteException("No hay stock suficiente de " + item.titulo() + ".");
    }

    static ResumenCompraDTO resumir(List<ItemDTO> items, BigDecimal porcentajeEnvio, int idCupon, BigDecimal porcentajeCupon) throws ValidacionException {
        BigDecimal originales = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        long cantidad = 0;
        for (ItemDTO item : items) {
            originales = originales.add(item.precioOriginal().multiply(BigDecimal.valueOf(item.cantidad())));
            subtotal = subtotal.add(item.subtotal());
            cantidad += item.cantidad();
            if (cantidad > Integer.MAX_VALUE) throw new ValidacionException("cantidad", "El carrito contiene demasiados ejemplares.");
        }
        BigDecimal descuento = subtotal.multiply(porcentajeCupon).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal envio = subtotal.multiply(porcentajeEnvio).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new ResumenCompraDTO(items, originales.setScale(2, RoundingMode.HALF_UP), originales.subtract(subtotal).setScale(2, RoundingMode.HALF_UP),
                idCupon, porcentajeCupon, descuento, porcentajeEnvio, envio, subtotal.subtract(descuento).add(envio).setScale(2, RoundingMode.HALF_UP), (int) cantidad);
    }

    static void validarImportes(ResumenCompraDTO resumen) throws ValidacionException {
        if (!importesPermitidos(resumen))
            throw new ValidacionException("importe", "El importe excede el limite permitido por pedido. Reduce la cantidad.");
    }

    static boolean importesPermitidos(ResumenCompraDTO resumen) {
        for (ItemDTO item : resumen.items())
            if (item.precioOriginal().compareTo(MAXIMO_PRECIO) > 0 || item.precioUnitario().compareTo(MAXIMO_PRECIO) > 0
                    || item.subtotal().compareTo(MAXIMO_IMPORTE) > 0) return false;
        return resumen.subtotalProductos().compareTo(MAXIMO_IMPORTE) <= 0 && resumen.descuentoLibros().compareTo(MAXIMO_IMPORTE) <= 0
                && resumen.descuentoCupon().compareTo(MAXIMO_IMPORTE) <= 0 && resumen.costoEnvio().compareTo(MAXIMO_IMPORTE) <= 0
                && resumen.total().compareTo(MAXIMO_IMPORTE) <= 0;
    }
}

