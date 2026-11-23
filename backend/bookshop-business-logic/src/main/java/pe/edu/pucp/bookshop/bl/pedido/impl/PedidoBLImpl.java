package pe.edu.pucp.bookshop.bl.pedido.impl;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.DatosCambiadosException;
import pe.edu.pucp.bookshop.bl.exception.NoEncontradoException;
import pe.edu.pucp.bookshop.bl.exception.OperacionNoPermitidaException;
import pe.edu.pucp.bookshop.bl.exception.PagoRechazadoException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.pedido.PedidoBL;
import pe.edu.pucp.bookshop.dao.catalogo.LibroDAO;
import pe.edu.pucp.bookshop.dao.catalogo.impl.LibroDAOImpl;
import pe.edu.pucp.bookshop.dao.configuracion.ConfiguracionDAO;
import pe.edu.pucp.bookshop.dao.configuracion.impl.ConfiguracionDAOImpl;
import pe.edu.pucp.bookshop.dao.pedido.CuponDAO;
import pe.edu.pucp.bookshop.dao.pedido.PedidoDAO;
import pe.edu.pucp.bookshop.dao.pedido.SolicitudDevolucionDAO;
import pe.edu.pucp.bookshop.dao.pedido.impl.CuponDAOImpl;
import pe.edu.pucp.bookshop.dao.pedido.impl.PedidoDAOImpl;
import pe.edu.pucp.bookshop.dao.pedido.impl.SolicitudDevolucionDAOImpl;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.catalogo.Libro;
import pe.edu.pucp.bookshop.model.comun.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;
import pe.edu.pucp.bookshop.model.configuracion.Configuracion;
import pe.edu.pucp.bookshop.model.pedido.Cupon;
import pe.edu.pucp.bookshop.model.pedido.DetallePedido;
import pe.edu.pucp.bookshop.model.pedido.EstadoPedido;
import pe.edu.pucp.bookshop.model.pedido.ItemCarrito;
import pe.edu.pucp.bookshop.model.pedido.LineaCompra;
import pe.edu.pucp.bookshop.model.pedido.Pedido;
import pe.edu.pucp.bookshop.model.pedido.PedidoDetalle;
import pe.edu.pucp.bookshop.model.pedido.ResumenCompra;
import pe.edu.pucp.bookshop.model.pedido.SolicitudCompra;
import pe.edu.pucp.bookshop.model.pedido.SolicitudDevolucion;
import pe.edu.pucp.bookshop.model.usuario.Direccion;
import pe.edu.pucp.bookshop.model.usuario.MetodoPago;
import pe.edu.pucp.bookshop.model.usuario.TipoMetodoPago;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PedidoBLImpl implements PedidoBL {

    private PedidoDAO pedidoDAO = new PedidoDAOImpl();
    private LibroDAO libroDAO = new LibroDAOImpl();
    private CuponDAO cuponDAO = new CuponDAOImpl();
    private SolicitudDevolucionDAO solicitudDAO = new SolicitudDevolucionDAOImpl();
    private ConfiguracionDAO configuracionDAO = new ConfiguracionDAOImpl();

    // RF11.4–5 y RF15.3: importes del carrito con los precios y descuentos vigentes.
    // RF11.1: solo entran los libros activos con stock, y cada uno hasta su stock.
    @Override
    public ResumenCompra calcularResumen(SolicitudCompra solicitud) throws BusinessLogicException {
        try {
            LocalDate hoy = LocalDate.now();
            Map<Integer, Libro> activos = new HashMap<>();
            for (Libro libro : libroDAO.listarActivos(hoy)) {
                activos.put(libro.getId(), libro);
            }

            List<LineaCompra> lineas = new ArrayList<>();
            double subtotal = 0;
            for (ItemCarrito item : solicitud.items()) {
                Libro libro = activos.get(item.idLibro());
                if (libro == null || libro.getStock() == 0) continue;
                int cantidad = Math.min(item.cantidad(), libro.getStock());
                double subtotalLinea = Math.round(libro.getPrecioFinal() * cantidad * 100) / 100.0;
                lineas.add(new LineaCompra(libro, cantidad, subtotalLinea));
                subtotal += subtotalLinea;
            }
            subtotal = Math.round(subtotal * 100) / 100.0;

            // El cupón se aplica solo si es utilizable (ERS §2.2): descuentoCupon = subtotal × porcentaje / 100
            Cupon cupon = null;
            double descuentoCupon = 0;
            if (solicitud.idCupon() != null) {
                Cupon elegido = cuponDAO.obtener(solicitud.idCupon());
                if (elegido.getActivo() && !hoy.isAfter(elegido.getFechaVencimiento())) {
                    cupon = elegido;
                    descuentoCupon = Math.round(subtotal * cupon.getPorcentaje()) / 100.0;
                }
            }
            double costoEnvio = configuracionDAO.obtener().getCostoEnvio();
            // total = subtotal − descuentoCupon + costoEnvio
            double total = Math.round((subtotal - descuentoCupon + costoEnvio) * 100) / 100.0;
            return new ResumenCompra(lineas, cupon, subtotal, descuentoCupon, costoEnvio, total);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF15.4–7 y RF16: revalida, simula el pago y registra el pedido, todo en una sola transacción (RN12)
    @Override
    public Pedido pagar(SolicitudCompra solicitud) throws BusinessLogicException {
        try {
            // Dos clientes pueden comprar a la vez el último ejemplar: se bloquean los libros del carrito hasta
            // terminar, y quien llega después espera y luego ve el stock ya descontado. Se bloquean siempre en
            // orden de id: si cada compra siguiera el orden de su carrito, dos compras podrían esperarse entre sí
            List<ItemCarrito> enOrden = new ArrayList<>(solicitud.items());
            enOrden.sort((a, b) -> a.idLibro() - b.idLibro());
            for (ItemCarrito item : enOrden) {
                libroDAO.bloquear(item.idLibro());
            }

            // RF15.4: se recalcula con los datos actuales y se compara con lo que vio el cliente
            ResumenCompra actual = calcularResumen(solicitud);
            List<String> cambios = buscarCambios(solicitud.preparado(), actual);
            if (!cambios.isEmpty()) {
                throw new DatosCambiadosException(String.join("\n", cambios));
            }

            // RF15.5: pago simulado, rechazado con un 20 % de probabilidad
            if (Math.random() < 0.2) {
                throw new PagoRechazadoException("Pago rechazado. Puedes intentarlo de nuevo o elegir otro método de pago.");
            }

            // RF16.1: el pedido, sus detalles con el precio y el descuento de este momento, el stock y el cupón
            LocalDateTime ahora = LocalDateTime.now().withNano(0);
            String codigo = UUID.randomUUID().toString().toUpperCase();
            Pedido pedido = new Pedido();
            Usuario usuario = new Usuario();
            usuario.setId(solicitud.idUsuario());
            pedido.setUsuario(usuario);
            Direccion direccion = new Direccion();
            direccion.setId(solicitud.idDireccion());
            pedido.setDireccion(direccion);
            MetodoPago metodo = new MetodoPago();
            metodo.setId(solicitud.idMetodoPago());
            pedido.setMetodoPago(metodo);
            pedido.setCupon(actual.cupon());
            pedido.setEstado(EstadoPedido.EN_ALMACEN);
            pedido.setFechaCompra(ahora);
            pedido.setCodigoTransaccion("TX-" + ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + codigo.substring(0, 4));
            pedido.setCodigoSeguimiento("SG-" + codigo.substring(24, 28) + "-" + codigo.substring(28, 32));
            pedido.setSubtotal(actual.subtotal());
            pedido.setDescuentoCupon(actual.descuentoCupon());
            pedido.setCostoEnvio(actual.costoEnvio());
            pedido.setTotal(actual.total());
            pedido.setContadoParaCupon(false);
            pedido.setId(pedidoDAO.insertar(pedido));

            for (LineaCompra linea : actual.lineas()) {
                DetallePedido detalle = new DetallePedido();
                detalle.setLibro(linea.libro());
                detalle.setCantidad(linea.cantidad());
                detalle.setPrecioUnitario(linea.libro().getPrecio());
                detalle.setDescuentoUnitario(Math.round((linea.libro().getPrecio() - linea.libro().getPrecioFinal()) * 100) / 100.0);
                detalle.setSubtotal(linea.subtotal());
                pedidoDAO.insertarDetalle(pedido.getId(), detalle);
                libroDAO.descontarStock(linea.libro().getId(), linea.cantidad());
            }
            if (actual.cupon() != null) {
                cuponDAO.desactivar(actual.cupon().getId());
            }
            TransactionContext.commit();
            return pedido;
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } catch (BusinessLogicException ex) {
            TransactionContext.rollback();
            throw ex;
        } finally {
            TransactionContext.close();
        }
    }

    // RF15.4: lo que cambió entre el resumen que vio el cliente y el actual, un texto por cambio
    // (un libro que ya no está, menos stock, otro precio, el cupón que ya no se puede usar u otro costo de envío)
    private List<String> buscarCambios(ResumenCompra preparado, ResumenCompra actual) {
        List<String> cambios = new ArrayList<>();
        for (LineaCompra antes : preparado.lineas()) {
            LineaCompra ahora = null;
            for (LineaCompra linea : actual.lineas()) {
                if (linea.libro().getId().equals(antes.libro().getId())) {
                    ahora = linea;
                }
            }
            String titulo = "«" + antes.libro().getTitulo() + "»";
            if (ahora == null) {
                cambios.add(titulo + " ya no está disponible.");
                continue;
            }
            if (ahora.cantidad() < antes.cantidad()) {
                cambios.add(ahora.cantidad() == 1 ? "Solo queda 1 unidad de " + titulo + "."
                        : "Solo quedan " + ahora.cantidad() + " unidades de " + titulo + ".");
            }
            if (!ahora.libro().getPrecioFinal().equals(antes.libro().getPrecioFinal())) {
                cambios.add("El precio de " + titulo + " cambió de "
                        + String.format(Locale.US, "S/ %,.2f", antes.libro().getPrecioFinal()) + " a "
                        + String.format(Locale.US, "S/ %,.2f", ahora.libro().getPrecioFinal()) + ".");
            }
        }
        if (preparado.cupon() != null && actual.cupon() == null) {
            cambios.add("El cupón " + preparado.cupon().getCodigo() + " ya no se puede usar.");
        }
        if (actual.costoEnvio() != preparado.costoEnvio()) {
            cambios.add("El costo de envío cambió de " + String.format(Locale.US, "S/ %,.2f", preparado.costoEnvio())
                    + " a " + String.format(Locale.US, "S/ %,.2f", actual.costoEnvio()) + ".");
        }
        return cambios;
    }

    // RF03.1–2: filtro por estado (null: todos) y orden por fecha de compra o por total
    @Override
    public ResultadoPagina<Pedido> listarPorUsuario(int idUsuario, EstadoPedido estado, String orden, int pagina, int tamano) throws BusinessLogicException {
        try {
            List<Pedido> pedidos = new ArrayList<>();
            for (Pedido pedido : pedidoDAO.listarPorUsuario(idUsuario)) {
                if (estado == null || pedido.getEstado() == estado) {
                    pedidos.add(pedido);
                }
            }
            if ("fecha-asc".equals(orden)) {
                pedidos.sort((a, b) -> a.getFechaCompra().compareTo(b.getFechaCompra()));
            } else if ("total-desc".equals(orden)) {
                pedidos.sort((a, b) -> Double.compare(b.getTotal(), a.getTotal()));
            } else if ("total-asc".equals(orden)) {
                pedidos.sort((a, b) -> Double.compare(a.getTotal(), b.getTotal()));
            } else {
                pedidos.sort((a, b) -> b.getFechaCompra().compareTo(a.getFechaCompra()));
            }
            int total = pedidos.size();
            int desde = Math.min((pagina - 1) * tamano, total);
            int hasta = Math.min(desde + tamano, total);
            return new ResultadoPagina<>(total, new ArrayList<>(pedidos.subList(desde, hasta)));
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF03.3, con lo que decide las acciones del pedido (RN07 y RN08)
    @Override
    public PedidoDetalle obtener(int id) throws BusinessLogicException {
        try {
            Pedido pedido = pedidoDAO.obtener(id);
            if (pedido == null) {
                throw new NoEncontradoException("No encontramos lo que buscas.");
            }
            pedido.setDetalles(pedidoDAO.listarDetalles(id));
            Configuracion configuracion = configuracionDAO.obtener();

            // RN07: cancelaciones del cliente en el mes calendario
            LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            int cancelacionesRestantes = configuracion.getMaxCancelacionesMes()
                    - pedidoDAO.contarCancelaciones(pedido.getUsuario().getId(), inicioMes);

            // RN08: dentro de diasDevolucion desde la entrega, hasta maxSolicitudesDevolucion y una pendiente a la vez
            LocalDate devolucionHasta = null;
            boolean devolucionEnPlazo = false;
            if (pedido.getFechaEntrega() != null) {
                LocalDateTime limite = pedido.getFechaEntrega().plusDays(configuracion.getDiasDevolucion());
                devolucionHasta = limite.toLocalDate();
                devolucionEnPlazo = !LocalDateTime.now().isAfter(limite);
            }
            List<SolicitudDevolucion> solicitudes = solicitudDAO.listarPorPedido(id);
            boolean devolucionPendiente = false;
            for (SolicitudDevolucion solicitud : solicitudes) {
                if (solicitud.getEstado() == EstadoSolicitud.PENDIENTE) {
                    devolucionPendiente = true;
                }
            }
            int solicitudesRestantes = configuracion.getMaxSolicitudesDevolucion() - solicitudes.size();
            return new PedidoDetalle(pedido, cancelacionesRestantes, devolucionHasta, devolucionEnPlazo,
                    devolucionPendiente, solicitudesRestantes);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF03.5 y RN07: solo en almacén y bajo el límite del mes; la cancelación repone el stock (RN09)
    @Override
    public void cancelar(int id) throws BusinessLogicException {
        try {
            PedidoDetalle detalle = obtener(id);
            if (detalle.pedido().getEstado() != EstadoPedido.EN_ALMACEN) {
                throw new OperacionNoPermitidaException("Solo se pueden cancelar pedidos en almacén.");
            }
            if (detalle.cancelacionesRestantes() <= 0) {
                throw new OperacionNoPermitidaException("Alcanzaste el límite de cancelaciones de este mes.");
            }
            pedidoDAO.cancelar(id, LocalDateTime.now().withNano(0));
            libroDAO.reponerStock(id);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } catch (BusinessLogicException ex) {
            TransactionContext.rollback();
            throw ex;
        } finally {
            TransactionContext.close();
        }
    }

    // RF17: comprobante con los importes históricos del pedido (plantilla reportes/comprobante.jrxml)
    @Override
    public byte[] generarComprobante(int id) throws BusinessLogicException {
        try {
            Pedido pedido = pedidoDAO.obtener(id);
            pedido.setDetalles(pedidoDAO.listarDetalles(id));

            Map<String, Object> parametros = new HashMap<>();
            parametros.put(JRParameter.REPORT_LOCALE, Locale.US);   // S/ 1,234.50 (UX-UI §12.1)
            parametros.put("numero", pedido.getId());
            parametros.put("fechaCompra", pedido.getFechaCompra().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            parametros.put("cliente", pedido.getUsuario().getNombreCompleto());
            parametros.put("correo", pedido.getUsuario().getCorreo());
            Direccion direccion = pedido.getDireccion();
            parametros.put("envio", direccion.getAlias() + " · " + direccion.getDireccion() + "\n"
                    + direccion.getDistrito() + ", " + direccion.getCiudad());
            // RF02.10: el número enmascarado, con los 4 últimos dígitos visibles
            MetodoPago metodo = pedido.getMetodoPago();
            String ultimos = metodo.getNumero().substring(metodo.getNumero().length() - 4);
            if (metodo.getTipo() == TipoMetodoPago.TARJETA) {
                parametros.put("pago", "Tarjeta •••• •••• •••• " + ultimos);
            } else {
                parametros.put("pago", metodo.getProveedor() + " ••••• " + ultimos);
            }
            parametros.put("transaccion", pedido.getCodigoTransaccion());
            parametros.put("seguimiento", pedido.getCodigoSeguimiento());
            parametros.put("subtotal", pedido.getSubtotal());
            if (pedido.getCupon() != null) {
                parametros.put("cupon", "Cupón " + pedido.getCupon().getCodigo() + " (" + pedido.getCupon().getPorcentaje() + " %)");
            }
            parametros.put("descuentoCupon", pedido.getDescuentoCupon());
            parametros.put("costoEnvio", pedido.getCostoEnvio());
            parametros.put("total", pedido.getTotal());

            JasperReport plantilla = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/reportes/comprobante.jasper"));
            JasperPrint documento = JasperFillManager.fillReport(plantilla, parametros, new JRBeanCollectionDataSource(pedido.getDetalles()));
            return JasperExportManager.exportReportToPdf(documento);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        } catch (JRException ex) {
            // Un error de la plantilla no es del uso normal: se informa como fallo del servidor
            throw new RuntimeException(ex);
        }
    }

    // RF03.4: desde el último cambio, con menos de 1 minuto no cambia; entre 1 y 2 minutos cambia con un 50 %
    // de probabilidad en cada revisión; con 2 minutos o más cambia siempre
    @Override
    public void avanzarSeguimiento() throws BusinessLogicException {
        try {
            LocalDateTime ahora = LocalDateTime.now().withNano(0);
            for (Pedido pedido : pedidoDAO.listarPorEstado(EstadoPedido.EN_ALMACEN)) {
                long segundos = Duration.between(pedido.getFechaCompra(), ahora).getSeconds();
                if (segundos >= 120 || (segundos >= 60 && Math.random() < 0.5)) {
                    pedidoDAO.registrarSalida(pedido.getId(), ahora);
                }
            }
            for (Pedido pedido : pedidoDAO.listarPorEstado(EstadoPedido.EN_CAMINO)) {
                long segundos = Duration.between(pedido.getFechaSalida(), ahora).getSeconds();
                if (segundos >= 120 || (segundos >= 60 && Math.random() < 0.5)) {
                    pedidoDAO.registrarEntrega(pedido.getId(), ahora);
                }
            }
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }
}
