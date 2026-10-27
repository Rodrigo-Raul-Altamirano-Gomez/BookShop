package me.Business.compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import me.Business.configuracion.ConfiguracionBOImpl;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.CondicionesCompraCambiaronException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PagoRechazadoException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.StockInsuficienteException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.Business.usuario.MetodosPago;
import me.Business.Validaciones;
import me.DAO.catalogo.DescuentoDAO;
import me.DAO.catalogo.DescuentoDAOImpl;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.solicitud.SolicitudDAO;
import me.DAO.solicitud.SolicitudDAOImpl;
import me.DAO.usuario.DireccionEnvioDAO;
import me.DAO.usuario.DireccionEnvioDAOImpl;
import me.DAO.usuario.MetodoPagoDAO;
import me.DAO.usuario.MetodoPagoDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DAO.venta.CuponDAO;
import me.DAO.venta.CuponDAOImpl;
import me.DAO.venta.PedidoDAO;
import me.DAO.venta.PedidoDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ConfiguracionDTO;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.DireccionEnvioDTO;
import me.Model.dto.compra.FiltroPedidoDTO;
import me.Model.dto.compra.ItemDTO;
import me.Model.dto.compra.PreparacionCompraDTO;
import me.Model.dto.compra.ResumenCompraDTO;
import me.Model.dto.compra.ResumenPedidoDTO;
import me.Model.dto.compra.SeleccionCompraDTO;
import me.Model.dto.compra.TarjetaPedidoDTO;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.compra.Cupon;
import me.Model.entities.compra.DireccionEnvio;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.compra.Item;
import me.Model.entities.compra.MetodoPago;
import me.Model.entities.compra.Pedido;
import me.Model.entities.usuario.Usuario;

public class PedidoBOImpl implements PedidoBO {
    private final PedidoDAO pedidos;
    private final LibroDAO libros;
    private final DescuentoDAO descuentos;
    private final UsuarioDAO usuarios;
    private final DireccionEnvioDAO direcciones;
    private final MetodoPagoDAO metodos;
    private final CuponDAO cupones;
    private final SolicitudDAO solicitudes;
    private final CarritoBOImpl carrito;
    private final Autorizacion autorizacion;
    private final Clock reloj;
    private final DoubleSupplier azar;
    private final ProveedorConfiguracion configuracion;

    // Los constructores por defecto comparten la memoria de CarritoBOImpl por actor.
    public PedidoBOImpl() {
        this(new PedidoDAOImpl(), new LibroDAOImpl(), new DescuentoDAOImpl(), new UsuarioDAOImpl(),
                new DireccionEnvioDAOImpl(), new MetodoPagoDAOImpl(), new CuponDAOImpl(), new SolicitudDAOImpl(),
                new CarritoBOImpl(), Clock.system(ZoneId.of("America/Lima")), Math::random, ConfiguracionBOImpl::actual);
    }

    // Al inyectar dependencias, se usa la misma instancia de carrito para navegación y compra.
    public PedidoBOImpl(PedidoDAO pedidos, LibroDAO libros, DescuentoDAO descuentos, UsuarioDAO usuarios,
            DireccionEnvioDAO direcciones, MetodoPagoDAO metodos, CuponDAO cupones, SolicitudDAO solicitudes,
            CarritoBOImpl carrito, Clock reloj, DoubleSupplier azar, ProveedorConfiguracion configuracion) {
        this.pedidos = pedidos;
        this.libros = libros;
        this.descuentos = descuentos;
        this.usuarios = usuarios;
        this.direcciones = direcciones;
        this.metodos = metodos;
        this.cupones = cupones;
        this.solicitudes = solicitudes;
        this.carrito = carrito;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
        this.azar = azar;
        this.configuracion = configuracion;
    }

    @Override
    public PreparacionCompraDTO preparar(int idActor) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        CarritoBOImpl.EstadoCarrito estado = carrito.estado(idActor);
        synchronized (estado) {
            ResumenCompraDTO resumen = carrito.resumen(estado);
            if (resumen.items().isEmpty()) throw new ConflictoException("El carrito esta vacio.");
            CarritoBOImpl.validarImportes(resumen);
            try {
                SeleccionCompraDTO seleccion = predeterminados(idActor);
                estado.preparacion = new PreparacionCompraDTO(resumen, seleccion, puedePagar(resumen, seleccion));
                estado.pedidoConfirmado = 0;
                return estado.preparacion;
            } catch (SQLException e) { throw new PersistenciaException("No se pudo preparar la compra.", e); }
        }
    }

    private SeleccionCompraDTO predeterminados(int idActor) throws SQLException {
        int direccion = 0;
        int metodo = 0;
        int pagina = 1;
        ResultadoPaginadoDTO<DireccionEnvioDTO> direccionesPagina;
        do {
            direccionesPagina = direcciones.listarPorUsuario(idActor, pagina++, 100);
            for (DireccionEnvioDTO candidata : direccionesPagina.items()) if (candidata.predeterminada()) direccion = candidata.id();
        } while (direccion == 0 && pagina <= direccionesPagina.totalPaginas());
        pagina = 1;
        ResultadoPaginadoDTO<MetodoPago> metodosPagina;
        do {
            metodosPagina = metodos.listarPorUsuario(idActor, pagina++, 100);
            for (MetodoPago candidato : metodosPagina.items())
                if (candidato.isPredeterminado() && candidato.validarEstado(LocalDateTime.now(reloj).toLocalDate())) metodo = candidato.getId();
        } while (metodo == 0 && pagina <= metodosPagina.totalPaginas());
        return new SeleccionCompraDTO(direccion, metodo, 0);
    }

    @Override
    public PreparacionCompraDTO consultarPreparacion(int idActor) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        CarritoBOImpl.EstadoCarrito estado = carrito.estado(idActor);
        synchronized (estado) {
            if (estado.preparacion == null) throw new ConflictoException("Debes volver a preparar la compra desde el carrito.");
            return estado.preparacion;
        }
    }

    @Override
    public PreparacionCompraDTO seleccionar(int idActor, SeleccionCompraDTO seleccion) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        if (seleccion == null || seleccion.idDireccionEnvio() < 0 || seleccion.idMetodoPago() < 0 || seleccion.idCupon() < 0)
            throw new ValidacionException("seleccion", "La seleccion no es valida.");
        CarritoBOImpl.EstadoCarrito estado = carrito.estado(idActor);
        synchronized (estado) {
            if (estado.preparacion == null || estado.pedidoConfirmado > 0) throw new ConflictoException("Debes preparar una compra vigente.");
            try {
                LocalDateTime ahora = LocalDateTime.now(reloj);
                if (seleccion.idDireccionEnvio() > 0) validarDireccion(direcciones.leer(seleccion.idDireccionEnvio()), idActor);
                if (seleccion.idMetodoPago() > 0) validarMetodo(metodos.leer(seleccion.idMetodoPago()), idActor, ahora);
                ResumenCompraDTO base = carrito.resumen(estado);
                Cupon cupon = seleccion.idCupon() == 0 ? null : cupones.leer(seleccion.idCupon());
                if (seleccion.idCupon() > 0) validarCupon(cupon, idActor, base.subtotal(), ahora);
                ResumenCompraDTO resumen = CarritoBOImpl.resumir(base.items(), base.porcentajeEnvio(), seleccion.idCupon(),
                        cupon == null ? BigDecimal.ZERO : cupon.getPorcentaje());
                CarritoBOImpl.validarImportes(resumen);
                // Se sustituye solo tras validar toda la selección; un error conserva la anterior.
                estado.preparacion = new PreparacionCompraDTO(resumen, seleccion, puedePagar(resumen, seleccion));
                return estado.preparacion;
            } catch (SQLException e) { throw new PersistenciaException("No se pudo validar la seleccion de compra.", e); }
        }
    }

    private boolean puedePagar(ResumenCompraDTO resumen, SeleccionCompraDTO seleccion) {
        if (seleccion.idDireccionEnvio() <= 0 || seleccion.idMetodoPago() <= 0 || resumen.items().isEmpty()
                || !CarritoBOImpl.importesPermitidos(resumen)) return false;
        for (ItemDTO item : resumen.items())
            if (!Boolean.TRUE.equals(item.activo()) || item.stockDisponible() == null || item.stockDisponible() < item.cantidad()) return false;
        return true;
    }

    private void validarDireccion(DireccionEnvio direccion, int idActor) throws NegocioException {
        if (direccion == null || !direccion.estaDisponible()) throw new ConflictoException("La direccion seleccionada ya no esta disponible.");
        autorizacion.propietario(idActor, direccion.getUsuario().getId());
    }

    private void validarMetodo(MetodoPago metodo, int idActor, LocalDateTime fecha) throws NegocioException {
        if (metodo == null) throw new ConflictoException("El metodo seleccionado no existe.");
        autorizacion.propietario(idActor, metodo.getUsuario().getId());
        if (!metodo.validarEstado(fecha.toLocalDate())) throw new ConflictoException("El metodo seleccionado esta vencido o retirado.");
    }

    private void validarCupon(Cupon cupon, int idActor, BigDecimal subtotal, LocalDateTime fecha) throws NegocioException {
        if (cupon == null) throw new ConflictoException("El cupon seleccionado no existe.");
        autorizacion.propietario(idActor, cupon.getUsuario().getId());
        if (!cupon.esAplicable(subtotal, fecha)) throw new ConflictoException("El cupon no es propio, vigente, disponible o no alcanza su minimo.");
    }

    @Override
    public DetallePedidoDTO pagar(int idActor) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        CarritoBOImpl.EstadoCarrito estado = carrito.estado(idActor);
        synchronized (estado) {
            if (estado.pedidoConfirmado > 0) return consultar(idActor, estado.pedidoConfirmado);
            PreparacionCompraDTO previa = estado.preparacion;
            if (previa == null) throw new ConflictoException("Debes preparar la compra desde el carrito.");
            if (previa.seleccion().idDireccionEnvio() <= 0 || previa.seleccion().idMetodoPago() <= 0)
                throw new ValidacionException("seleccion", "Selecciona direccion y metodo de pago.");
            try {
                TransactionContext.getConnection();
                Usuario usuario = usuarios.leerParaActualizar(idActor);
                if (usuario == null || !usuario.estaDisponible()) throw new AccesoDenegadoException("La cuenta ya no esta disponible.");
                LocalDateTime ahora = LocalDateTime.now(reloj);
                List<ItemDTO> items = consultarItemsBloqueados(previa.resumen().items(), ahora);
                ConfiguracionDTO parametros = configuracion.obtener();
                DireccionEnvio direccion = direcciones.leerParaActualizar(previa.seleccion().idDireccionEnvio());
                MetodoPago metodo = metodos.leerParaActualizar(previa.seleccion().idMetodoPago());
                validarDireccion(direccion, idActor);
                validarMetodo(metodo, idActor, ahora);
                Cupon cupon = previa.seleccion().idCupon() == 0 ? null : cupones.leerParaActualizar(previa.seleccion().idCupon());
                ResumenCompraDTO base = CarritoBOImpl.resumir(items, parametros.porcentajeEnvio(), 0, BigDecimal.ZERO);
                if (previa.seleccion().idCupon() > 0) validarCupon(cupon, idActor, base.subtotal(), ahora);
                ResumenCompraDTO vigente = CarritoBOImpl.resumir(items, parametros.porcentajeEnvio(), previa.seleccion().idCupon(),
                        cupon == null ? BigDecimal.ZERO : cupon.getPorcentaje());
                estado.preparacion = new PreparacionCompraDTO(vigente, previa.seleccion(), puedePagar(vigente, previa.seleccion()));
                CarritoBOImpl.validarImportes(vigente);
                for (ItemDTO item : items) CarritoBOImpl.validarDisponible(item);
                if (condicionesCambiaron(previa.resumen(), vigente))
                    throw new CondicionesCompraCambiaronException("Los precios o importes cambiaron. Revisa el nuevo resumen antes de pagar.");
                if (azar.getAsDouble() >= 0.90) throw new PagoRechazadoException("El pago simulado fue rechazado. Puedes volver a intentarlo.");
                for (ItemDTO item : items)
                    if (!libros.descontarStock(item.idLibro(), item.cantidad(), usuario.getEmail()))
                        throw new StockInsuficienteException("El stock cambio antes de registrar la compra.");
                if (cupon != null && !cupones.consumir(cupon.getId(), idActor, ahora, usuario.getEmail()))
                    throw new ConflictoException("El cupon ya no esta disponible.");
                Pedido pedido = crearPedido(usuario, direccion, metodo, cupon, vigente, ahora);
                pedidos.insertar(pedido);
                TransactionContext.commit();
                // El replay conserva este id durante la vida del estado en memoria; no sobrevive a un reinicio.
                // Se registra antes de construir la respuesta: un fallo posterior no permite cobrar otra vez.
                estado.pedidoConfirmado = pedido.getId();
                estado.lineas.clear();
            } catch (SQLException e) {
                TransactionContext.rollback();
                throw new PersistenciaException("No se pudo registrar la compra. Sus efectos se revirtieron.", e);
            } catch (NegocioException | RuntimeException e) {
                TransactionContext.rollback();
                if (e instanceof NegocioException negocio) throw negocio;
                throw new PersistenciaException("No se pudo completar la compra.", e);
            } finally { TransactionContext.close(); }
            return consultar(idActor, estado.pedidoConfirmado);
        }
    }

    private List<ItemDTO> consultarItemsBloqueados(List<ItemDTO> originales, LocalDateTime fecha) throws SQLException, NegocioException {
        // Orden común al bloquear evita que dos compras de los mismos libros inviertan los bloqueos.
        List<ItemDTO> ordenados = new ArrayList<>(originales);
        ordenados.sort(Comparator.comparingInt(ItemDTO::idLibro));
        List<ItemDTO> actuales = new ArrayList<>();
        for (ItemDTO anterior : ordenados) {
            Libro libro = libros.leerParaActualizar(anterior.idLibro());
            if (libro == null) throw new ConflictoException("Uno de los libros ya no esta disponible.");
            Descuento descuento = descuentos.obtenerVigente(libro.getId(), fecha);
            BigDecimal porcentaje = descuento == null ? BigDecimal.ZERO : descuento.getPorcentaje();
            BigDecimal unitario = libro.getPrecioBase().multiply(BigDecimal.valueOf(100).subtract(porcentaje))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            actuales.add(new ItemDTO(libro.getId(), libro.getTitulo(), new ImagenDTO(libro.getPortada(), libro.getTipoPortada()), anterior.cantidad(),
                    libro.getPrecioBase(), unitario, porcentaje, unitario.multiply(BigDecimal.valueOf(anterior.cantidad())), libro.isActivo(), libro.getStockFisico()));
        }
        List<ItemDTO> enOrdenOriginal = new ArrayList<>();
        for (ItemDTO original : originales)
            for (ItemDTO actual : actuales) if (original.idLibro() == actual.idLibro()) { enOrdenOriginal.add(actual); break; }
        return enOrdenOriginal;
    }

    private boolean condicionesCambiaron(ResumenCompraDTO anterior, ResumenCompraDTO actual) {
        if (anterior.porcentajeEnvio().compareTo(actual.porcentajeEnvio()) != 0
                || anterior.porcentajeCupon().compareTo(actual.porcentajeCupon()) != 0
                || anterior.total().compareTo(actual.total()) != 0 || anterior.items().size() != actual.items().size()) return true;
        for (int i = 0; i < anterior.items().size(); i++) {
            ItemDTO antes = anterior.items().get(i);
            ItemDTO ahora = actual.items().get(i);
            if (antes.idLibro() != ahora.idLibro() || antes.cantidad() != ahora.cantidad()
                    || antes.precioOriginal().compareTo(ahora.precioOriginal()) != 0
                    || antes.precioUnitario().compareTo(ahora.precioUnitario()) != 0
                    || antes.porcentajeDescuento().compareTo(ahora.porcentajeDescuento()) != 0) return true;
        }
        return false;
    }

    private Pedido crearPedido(Usuario usuario, DireccionEnvio direccion, MetodoPago metodo, Cupon cupon,
            ResumenCompraDTO resumen, LocalDateTime fecha) {
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setDireccionEnvio(direccion);
        pedido.setMetodoPago(metodo);
        pedido.setCupon(cupon);
        pedido.setSubtotalProductos(resumen.subtotalProductos());
        pedido.setDescuentoLibros(resumen.descuentoLibros());
        pedido.setPorcentajeCupon(resumen.porcentajeCupon());
        pedido.setDescuentoCupon(resumen.descuentoCupon());
        pedido.setPorcentajeEnvio(resumen.porcentajeEnvio());
        pedido.setCostoEnvio(resumen.costoEnvio());
        pedido.setTotal(resumen.total());
        pedido.setCantidadTotalLibros(resumen.cantidadLibros());
        pedido.setEstado(EstadoPedido.EN_ALMACEN);
        pedido.setFechaPago(fecha);
        pedido.setFechaCreacion(fecha);
        pedido.setUsuarioCreacion(usuario.getEmail());
        pedido.setIdentificadorTransaccion(UUID.randomUUID().toString());
        pedido.setCodigoSeguimiento(UUID.randomUUID().toString());
        pedido.setFechaEntregaEstimada(fecha.plusSeconds(90));
        List<Item> items = new ArrayList<>();
        for (ItemDTO datos : resumen.items()) {
            Item item = new Item();
            Libro libro = new Libro(); libro.setId(datos.idLibro());
            item.setLibro(libro);
            item.setCantidad(datos.cantidad());
            item.setPrecioBase(datos.precioOriginal());
            item.setPrecioUnitario(datos.precioUnitario());
            item.setPorcentajeDescuento(datos.porcentajeDescuento());
            item.setSubTotal(datos.subtotal());
            item.setFechaCreacion(fecha);
            item.setUsuarioCreacion(usuario.getEmail());
            items.add(item);
        }
        pedido.setItems(items);
        return pedido;
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaPedidoDTO> listar(int idActor, FiltroPedidoDTO filtro) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        if (filtro == null) filtro = new FiltroPedidoDTO();
        Validaciones.pagina(filtro.pagina(), filtro.tamanioPagina());
        if (filtro.estados().contains(EstadoPedido.EN_ESPERA)) throw new ValidacionException("estados", "En espera no pertenece al historial.");
        try { return pedidos.listarPorUsuario(idActor, filtro); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los pedidos.", e); }
    }

    @Override
    public DetallePedidoDTO consultar(int idActor, int idPedido) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        try {
            Pedido pedido = pedidos.leer(idPedido);
            if (pedido == null || pedido.isEliminado()) throw new RecursoNoEncontradoException("El pedido no existe.");
            autorizacion.propietario(idActor, pedido.getUsuario().getId());
            return detalle(pedido, true);
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar el pedido.", e); }
    }

    private DetallePedidoDTO detalle(Pedido pedido, boolean acciones) throws SQLException, NegocioException {
        List<ItemDTO> items = pedidos.obtenerItemsDTO(pedido.getId());
        ResumenCompraDTO resumen = new ResumenCompraDTO(items, pedido.getSubtotalProductos(), pedido.getDescuentoLibros(),
                pedido.getCupon() == null ? 0 : pedido.getCupon().getId(), pedido.getPorcentajeCupon(), pedido.getDescuentoCupon(),
                pedido.getPorcentajeEnvio(), pedido.getCostoEnvio(), pedido.getTotal(), pedido.getCantidadTotalLibros());
        DireccionEnvio direccion = direcciones.leer(pedido.getDireccionEnvio().getId());
        MetodoPago metodo = metodos.leer(pedido.getMetodoPago().getId());
        if (direccion == null || metodo == null) throw new RecursoNoEncontradoException("Faltan referencias del pedido.");
        DireccionEnvioDTO direccionDTO = new DireccionEnvioDTO(direccion.getId(), direccion.getAlias(), direccion.getCalle(),
                direccion.getDistrito(), direccion.getCiudad(), direccion.getReferencia(), direccion.isPredeterminada());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        boolean puedeDevolver = false;
        if (acciones && pedido.getEstado() == EstadoPedido.ENTREGADO) {
            ConfiguracionDTO parametros = configuracion.obtener();
            puedeDevolver = pedido.puedeSolicitarDevolucion(ahora, parametros.plazoDiasDevolucion(), parametros.maximoIntentosDevolucion())
                    && !solicitudes.existeDevolucionPendiente(pedido.getId());
        }
        boolean seguimiento = pedido.getEstado() != EstadoPedido.CANCELADO && pedido.getEstado() != EstadoPedido.DEVUELTO;
        return new DetallePedidoDTO(new ResumenPedidoDTO(pedido.getId(), pedido.getFechaPago(), pedido.getEstado(), pedido.getCantidadTotalLibros(), pedido.getTotal()),
                resumen, direccionDTO, MetodosPago.aDTO(metodo, ahora.toLocalDate()), pedido.getIdentificadorTransaccion(),
                seguimiento ? pedido.getCodigoSeguimiento() : null, seguimiento ? pedido.getFechaEntregaEstimada() : null,
                seguimiento ? pedido.getFechaSalida() : null, seguimiento ? pedido.getFechaEntrega() : null,
                acciones && pedido.puedeCancelar(), puedeDevolver);
    }

    // Reutilización interna de la consulta modal desde una solicitud ya autorizada.
    public DetallePedidoDTO consultarReferencia(int idActor, int idPedido) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            Pedido pedido = pedidos.leer(idPedido);
            if (pedido == null || pedido.isEliminado()) throw new RecursoNoEncontradoException("El pedido no existe.");
            if (actor.getId() != pedido.getUsuario().getId()) autorizacion.administrador(idActor);
            return detalle(pedido, false);
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar la referencia del pedido.", e); }
    }

    @Override
    public DetallePedidoDTO actualizarSeguimiento(int idActor, int idPedido) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            TransactionContext.getConnection();
            Pedido pedido = pedidos.leerParaActualizar(idPedido);
            verificarPropio(pedido, idActor);
            LocalDateTime ahora = LocalDateTime.now(reloj);
            EstadoPedido anterior = pedido.getEstado();
            if (anterior == EstadoPedido.EN_ALMACEN && !ahora.isBefore(pedido.getFechaPago().plusSeconds(30))) {
                pedido.setEstado(EstadoPedido.EN_CAMINO);
                pedido.setFechaSalida(ahora);
            } else if (anterior == EstadoPedido.EN_CAMINO && pedido.getFechaSalida() != null && !ahora.isBefore(pedido.getFechaSalida().plusSeconds(60))) {
                pedido.setEstado(EstadoPedido.ENTREGADO);
                pedido.setFechaEntrega(ahora);
            }
            if (pedido.getEstado() != anterior) {
                pedido.setUsuarioActualizacion(actor.getEmail());
                pedido.setFechaActualizacion(ahora);
                if (!pedidos.actualizarEstado(pedido, anterior)) throw new ConflictoException("El estado del pedido cambio.");
                if (pedido.getEstado() == EstadoPedido.ENTREGADO) premiarPrimeraEntrega(pedido, actor, ahora);
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo actualizar el seguimiento.", e);
        } catch (NegocioException | RuntimeException e) {
            TransactionContext.rollback();
            if (e instanceof NegocioException negocio) throw negocio;
            throw new PersistenciaException("No se pudo actualizar el seguimiento.", e);
        } finally { TransactionContext.close(); }
        return consultar(idActor, idPedido);
    }

    private void verificarPropio(Pedido pedido, int idActor) throws NegocioException {
        if (pedido == null || pedido.isEliminado()) throw new RecursoNoEncontradoException("El pedido no existe.");
        autorizacion.propietario(idActor, pedido.getUsuario().getId());
    }

    private void premiarPrimeraEntrega(Pedido pedido, Usuario actor, LocalDateTime fecha) throws SQLException, NegocioException {
        ConfiguracionDTO parametros = configuracion.obtener();
        Usuario propietario = usuarios.leerParaActualizar(pedido.getUsuario().getId());
        if (propietario == null || !propietario.estaDisponible()) throw new AccesoDenegadoException("La cuenta ya no esta disponible.");
        if (!usuarios.incrementarPedidosEntregados(propietario.getId(), actor.getEmail()))
            throw new ConflictoException("No se pudo contar la primera entrega.");
        int entregados = Math.addExact(propietario.getCantidadPedidosEntregados(), 1);
        if (entregados % parametros.pedidosEntregadosPorCupon() == 0) {
            Cupon cupon = new Cupon();
            cupon.setUsuario(propietario);
            cupon.setPorcentaje(parametros.porcentajeCupon());
            cupon.setMontoMinimo(parametros.montoMinimoCupon());
            cupon.setFechaInicio(fecha);
            cupon.setFechaFin(fecha.plusDays(parametros.vigenciaDiasCupon()));
            cupon.setFechaCreacion(fecha);
            cupon.setUsuarioCreacion(actor.getEmail());
            cupones.insertar(cupon);
        }
    }

    @Override
    public DetallePedidoDTO cancelar(int idActor, int idPedido, String motivo) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        Validaciones.texto(motivo, "motivo", 1000);
        try {
            TransactionContext.getConnection();
            Pedido pedido = pedidos.leerParaActualizar(idPedido);
            verificarPropio(pedido, idActor);
            if (!pedido.puedeCancelar()) throw new ConflictoException("Solo puedes cancelar un pedido en almacen.");
            if (azar.getAsDouble() >= 0.80) throw new ConflictoException("La cancelacion simulada fue rechazada. El pedido se conserva.");
            LocalDateTime ahora = LocalDateTime.now(reloj);
            pedido.setEstado(EstadoPedido.CANCELADO);
            pedido.setUsuarioActualizacion(actor.getEmail());
            pedido.setFechaActualizacion(ahora);
            if (!pedidos.actualizarEstado(pedido, EstadoPedido.EN_ALMACEN)) throw new ConflictoException("El pedido ya cambio de estado.");
            List<Item> items = pedido.getItems();
            items.sort(Comparator.comparingInt(item -> item.getLibro().getId()));
            for (Item item : items)
                if (!libros.reponerStock(item.getLibro().getId(), item.getCantidad(), actor.getEmail()))
                    throw new ConflictoException("No se pudo reponer el stock del pedido.");
            if (pedido.getCupon() != null) cupones.liberar(pedido.getCupon().getId(), ahora, actor.getEmail());
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cancelar el pedido.", e);
        } catch (NegocioException | RuntimeException e) {
            TransactionContext.rollback();
            if (e instanceof NegocioException negocio) throw negocio;
            throw new PersistenciaException("No se pudo cancelar el pedido.", e);
        } finally { TransactionContext.close(); }
        // El total del DTO permite anunciar el reembolso íntegro como mensaje temporal.
        return consultar(idActor, idPedido);
    }
}
