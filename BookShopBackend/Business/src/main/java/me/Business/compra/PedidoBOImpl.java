package me.Business.compra;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.Business.configuracion.ConfiguracionBOImpl;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.usuario.MetodosPago;
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
import me.DAO.venta.ItemPedidoDAO;
import me.DAO.venta.ItemPedidoDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ConfiguracionDTO;
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
import me.Model.entities.catalogo.Libro;
import me.Model.entities.compra.Cupon;
import me.Model.entities.compra.DireccionEnvio;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.compra.Item;
import me.Model.entities.compra.MetodoPago;
import me.Model.entities.compra.Pedido;
import me.Model.entities.usuario.Usuario;

public class PedidoBOImpl implements PedidoBO {

    private final PedidoDAO pedidoDAO = new PedidoDAOImpl();
    private final ItemPedidoDAO itemPedidoDAO = new ItemPedidoDAOImpl();
    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final DireccionEnvioDAO direccionEnvioDAO = new DireccionEnvioDAOImpl();
    private final MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAOImpl();
    private final CuponDAO cuponDAO = new CuponDAOImpl();
    private final SolicitudDAO solicitudDAO = new SolicitudDAOImpl();
    private final CarritoBOImpl carrito = new CarritoBOImpl();

    @Override
    public PreparacionCompraDTO preparar(int idActor) throws NegocioException {
        CarritoBOImpl.EstadoCarrito carritoUsuario = carrito.obtenerCarrito(idActor);
        synchronized (carritoUsuario) {
            ResumenCompraDTO resumen = carrito.consultarResumen(carritoUsuario);
            if (resumen.items().isEmpty())
                throw new NegocioException("El carrito esta vacio.");
            try {
                SeleccionCompraDTO seleccion = buscarDatosPredeterminados(idActor);
                carritoUsuario.preparacion = new PreparacionCompraDTO(resumen, seleccion, puedePagar(resumen,
                    seleccion));
                carritoUsuario.pedidoConfirmado = 0;
                return carritoUsuario.preparacion;
            } catch (SQLException e) {
                throw new PersistenciaException("No se pudo preparar la compra.", e);
            }
        }
    }

    private SeleccionCompraDTO buscarDatosPredeterminados(int idActor) throws SQLException {
        int direccion = 0;
        int metodo = 0;
        // Ambos listados colocan primero el registro predeterminado, cuando existe.
        ResultadoPaginadoDTO<DireccionEnvioDTO> direcciones = direccionEnvioDAO.listarPorUsuario(idActor, 1, 1);
        if (!direcciones.items().isEmpty() && direcciones.items().getFirst().predeterminada())
            direccion = direcciones.items().getFirst().id();
        ResultadoPaginadoDTO<MetodoPago> metodos = metodoPagoDAO.listarPorUsuario(idActor, 1, 1);
        if (!metodos.items().isEmpty()) {
            MetodoPago candidato = metodos.items().getFirst();
            if (candidato.isPredeterminado() && candidato.validarEstado(LocalDateTime.now().toLocalDate()))
                metodo = candidato.getId();
        }
        return new SeleccionCompraDTO(direccion, metodo, 0);
    }

    @Override
    public PreparacionCompraDTO consultarPreparacion(int idActor) throws NegocioException {
        CarritoBOImpl.EstadoCarrito carritoUsuario = carrito.obtenerCarrito(idActor);
        synchronized (carritoUsuario) {
            if (carritoUsuario.preparacion == null)
                throw new NegocioException("Debes volver a preparar la compra desde el carrito.");
            return carritoUsuario.preparacion;
        }
    }

    @Override
    public PreparacionCompraDTO seleccionar(int idActor, SeleccionCompraDTO seleccion) throws NegocioException {
        if (seleccion == null)
            throw new ValidacionException("seleccion", "La seleccion no es valida.");
        CarritoBOImpl.EstadoCarrito carritoUsuario = carrito.obtenerCarrito(idActor);
        synchronized (carritoUsuario) {
            if (carritoUsuario.preparacion == null || carritoUsuario.pedidoConfirmado > 0)
                throw new NegocioException("Debes preparar una compra vigente.");
            try {
                ResumenCompraDTO base = carrito.consultarResumen(carritoUsuario);
                Cupon cupon = validarSeleccion(idActor, seleccion, base.subtotal());
                ResumenCompraDTO resumen = aplicarCupon(base, cupon);
                // Solo se guarda si toda la selección es válida; un error conserva la anterior.
                carritoUsuario.preparacion = new PreparacionCompraDTO(resumen, seleccion, puedePagar(resumen,
                    seleccion));
                return carritoUsuario.preparacion;
            } catch (SQLException e) {
                throw new PersistenciaException("No se pudo validar la seleccion de compra.", e);
            }
        }
    }

    // Comprueba que la dirección, el método y el cupón elegidos sean del usuario y sigan vigentes.
    // Un id 0 significa "sin elegir". Devuelve el cupón elegido o null.
    private Cupon validarSeleccion(int idActor, SeleccionCompraDTO seleccion, BigDecimal subtotal)
        throws SQLException, NegocioException {
        LocalDateTime ahora = LocalDateTime.now();
        if (seleccion.idDireccionEnvio() > 0) {
            DireccionEnvio direccion = direccionEnvioDAO.leer(seleccion.idDireccionEnvio());
            if (direccion == null || !direccion.estaDisponible())
                throw new NegocioException("La direccion seleccionada ya no esta disponible.");
            if (direccion.getUsuario().getId() != idActor)
                throw new AccesoDenegadoException("La direccion pertenece a otra cuenta.");
        }
        if (seleccion.idMetodoPago() > 0) {
            MetodoPago metodo = metodoPagoDAO.leer(seleccion.idMetodoPago());
            if (metodo == null || metodo.getUsuario().getId() != idActor)
                throw new AccesoDenegadoException("El metodo seleccionado no pertenece a tu cuenta.");
            if (!metodo.validarEstado(ahora.toLocalDate()))
                throw new NegocioException("El metodo seleccionado esta vencido o retirado.");
        }
        if (seleccion.idCupon() == 0)
            return null;
        Cupon cupon = cuponDAO.leer(seleccion.idCupon());
        if (cupon == null || cupon.getUsuario().getId() != idActor)
            throw new AccesoDenegadoException("El cupon seleccionado no pertenece a tu cuenta.");
        if (!cupon.esAplicable(subtotal, ahora))
            throw new NegocioException("El cupon no esta vigente o no alcanza el minimo.");
        return cupon;
    }

    private ResumenCompraDTO aplicarCupon(ResumenCompraDTO base, Cupon cupon) {
        if (cupon == null)
            return base;
        return CarritoBOImpl.calcularResumen(base.items(), base.porcentajeEnvio(), cupon.getId(),
            cupon.getPorcentaje());
    }

    private boolean puedePagar(ResumenCompraDTO resumen, SeleccionCompraDTO seleccion) {
        if (seleccion.idDireccionEnvio() <= 0 || seleccion.idMetodoPago() <= 0 || resumen.items().isEmpty())
            return false;
        for (ItemDTO item : resumen.items()) {
            if (!item.activo() || item.stockDisponible() < item.cantidad())
                return false;
        }
        return true;
    }

    @Override
    public DetallePedidoDTO pagar(int idActor) throws NegocioException {
        CarritoBOImpl.EstadoCarrito carritoUsuario = carrito.obtenerCarrito(idActor);
        synchronized (carritoUsuario) {
            // Si la compra ya se confirmó, un reintento devuelve el mismo pedido sin cobrar de nuevo.
            if (carritoUsuario.pedidoConfirmado > 0)
                return consultar(idActor, carritoUsuario.pedidoConfirmado);
            PreparacionCompraDTO previa = carritoUsuario.preparacion;
            if (previa == null)
                throw new NegocioException("Debes preparar la compra desde el carrito.");
            SeleccionCompraDTO seleccion = previa.seleccion();
            if (seleccion.idDireccionEnvio() <= 0 || seleccion.idMetodoPago() <= 0)
                throw new ValidacionException("seleccion", "Selecciona direccion y metodo de pago.");
            try {
                Usuario usuario = usuarioDAO.leer(idActor);
                if (usuario == null || !usuario.estaDisponible())
                    throw new AccesoDenegadoException("La cuenta ya no esta disponible.");
                // 1. Se vuelven a comprobar la selección, los precios y el stock antes de cobrar.
                ResumenCompraDTO base = carrito.consultarResumen(carritoUsuario);
                Cupon cupon = validarSeleccion(idActor, seleccion, base.subtotal());
                ResumenCompraDTO vigente = aplicarCupon(base, cupon);
                carritoUsuario.preparacion = new PreparacionCompraDTO(vigente, seleccion, puedePagar(vigente,
                    seleccion));
                for (ItemDTO item : vigente.items()) {
                    if (!item.activo())
                        throw new NegocioException("Uno de los libros no esta disponible.");
                    if (item.stockDisponible() < item.cantidad())
                        throw new NegocioException("No hay stock suficiente de " + item.titulo() + ".");
                }
                if (previa.resumen().total().compareTo(vigente.total()) != 0)
                    throw new NegocioException("Los precios o importes cambiaron. Revisa el nuevo resumen" +
                        " antes de pagar.");
                // 2. Pago simulado con 90 % de éxito.
                if (Math.random() >= 0.90)
                    throw new NegocioException("El pago simulado fue rechazado. Puedes volver a intentarlo.");
                // 3. Se guardan en una sola transacción el stock, el cupón, el pedido y sus líneas.
                LocalDateTime ahora = LocalDateTime.now();
                for (ItemDTO item : vigente.items()) {
                    if (!libroDAO.descontarStock(item.idLibro(), item.cantidad(), usuario.getEmail()))
                        throw new NegocioException("El stock cambio antes de registrar la compra.");
                }
                if (cupon != null && !cuponDAO.consumir(cupon.getId(), idActor, ahora, usuario.getEmail()))
                    throw new NegocioException("El cupon ya no esta disponible.");
                DireccionEnvio direccion = direccionEnvioDAO.leer(seleccion.idDireccionEnvio());
                MetodoPago metodo = metodoPagoDAO.leer(seleccion.idMetodoPago());
                Pedido pedido = crearPedidoConItems(usuario, direccion, metodo, cupon, vigente, ahora);
                pedidoDAO.insertar(pedido);
                for (Item item : pedido.getItems())
                    itemPedidoDAO.insertar(pedido.getId(), item);
                TransactionContext.commit();
                carritoUsuario.pedidoConfirmado = pedido.getId();
                carritoUsuario.lineas.clear();
            } catch (SQLException e) {
                TransactionContext.rollback();
                throw new PersistenciaException("No se pudo registrar la compra. Sus efectos se revirtieron.", e);
            } catch (NegocioException e) {
                TransactionContext.rollback();
                throw e;
            } finally {
                TransactionContext.close();
            }
            return consultar(idActor, carritoUsuario.pedidoConfirmado);
        }
    }

    private Pedido crearPedidoConItems(Usuario usuario, DireccionEnvio direccion, MetodoPago metodo, Cupon
        cupon, ResumenCompraDTO resumen, LocalDateTime fecha) {
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
            Libro libro = new Libro();
            libro.setId(datos.idLibro());
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
    public ResultadoPaginadoDTO<TarjetaPedidoDTO> listar(int idActor, FiltroPedidoDTO filtro) throws
        NegocioException {
        if (filtro == null)
            filtro = FiltroPedidoDTO.porDefecto();
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La pagina y su tamaño deben ser positivos.");
        if (filtro.estados().contains(EstadoPedido.EN_ESPERA))
            throw new ValidacionException("estados", "En espera no pertenece al historial.");
        try {
            return pedidoDAO.listarPorUsuario(idActor, filtro);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los pedidos.", e);
        }
    }

    @Override
    public DetallePedidoDTO consultar(int idActor, int idPedido) throws NegocioException {
        try {
            Pedido pedido = pedidoDAO.leer(idPedido);
            if (pedido == null || pedido.isEliminado())
                throw new RecursoNoEncontradoException("El pedido no existe.");
            if (pedido.getUsuario().getId() != idActor)
                throw new AccesoDenegadoException("El pedido pertenece a otra cuenta.");
            return detalle(pedido, true);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el pedido.", e);
        }
    }

    private DetallePedidoDTO detalle(Pedido pedido, boolean acciones) throws SQLException, NegocioException {
        List<ItemDTO> items = itemPedidoDAO.obtenerItemsDTO(pedido.getId());
        ResumenCompraDTO resumen = new ResumenCompraDTO(items, pedido.getSubtotalProductos(),
            pedido.getDescuentoLibros(), pedido.getCupon() == null ? 0 : pedido.getCupon().getId(),
            pedido.getPorcentajeCupon(), pedido.getDescuentoCupon(), pedido.getPorcentajeEnvio(),
            pedido.getCostoEnvio(), pedido.getTotal(), pedido.getCantidadTotalLibros());
        DireccionEnvio direccion = direccionEnvioDAO.leer(pedido.getDireccionEnvio().getId());
        MetodoPago metodo = metodoPagoDAO.leer(pedido.getMetodoPago().getId());
        if (direccion == null || metodo == null)
            throw new RecursoNoEncontradoException("Faltan referencias del pedido.");
        DireccionEnvioDTO direccionDTO = new DireccionEnvioDTO(direccion.getId(), direccion.getAlias(),
            direccion.getCalle(), direccion.getDistrito(), direccion.getCiudad(), direccion.getReferencia(),
            direccion.isPredeterminada());
        LocalDateTime ahora = LocalDateTime.now();
        boolean puedeDevolver = false;
        if (acciones && pedido.getEstado() == EstadoPedido.ENTREGADO) {
            ConfiguracionDTO parametros = new ConfiguracionBOImpl().obtener();
            puedeDevolver = pedido.puedeSolicitarDevolucion(ahora, parametros.plazoDiasDevolucion(),
                parametros.maximoIntentosDevolucion()) && !solicitudDAO.existeDevolucionPendiente(pedido.getId());
        }
        boolean seguimiento = pedido.getEstado() != EstadoPedido.CANCELADO && pedido.getEstado() !=
            EstadoPedido.DEVUELTO;
        return new DetallePedidoDTO(new ResumenPedidoDTO(pedido.getId(), pedido.getFechaPago(),
            pedido.getEstado(), pedido.getCantidadTotalLibros(), pedido.getTotal()), resumen, direccionDTO,
            MetodosPago.aDTO(metodo, ahora.toLocalDate()), pedido.getIdentificadorTransaccion(), seguimiento ?
            pedido.getCodigoSeguimiento() : null, seguimiento ? pedido.getFechaEntregaEstimada() : null,
            seguimiento ? pedido.getFechaSalida() : null, seguimiento ? pedido.getFechaEntrega() : null,
            acciones && pedido.puedeCancelar(), puedeDevolver);
    }

    // Reutilización interna de la consulta modal desde una solicitud ya autorizada.
    @Override
    public DetallePedidoDTO consultarReferencia(int idPedido) throws NegocioException {
        try {
            Pedido pedido = pedidoDAO.leer(idPedido);
            if (pedido == null || pedido.isEliminado())
                throw new RecursoNoEncontradoException("El pedido no existe.");
            return detalle(pedido, false);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la referencia del pedido.", e);
        }
    }

    @Override
    public DetallePedidoDTO actualizarSeguimiento(int idActor, int idPedido) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            TransactionContext.getConnection();
            Pedido pedido = pedidoDAO.leer(idPedido);
            if (pedido == null || pedido.isEliminado())
                throw new RecursoNoEncontradoException("El pedido no existe.");
            if (pedido.getUsuario().getId() != idActor)
                throw new AccesoDenegadoException("El pedido pertenece a otra cuenta.");
            LocalDateTime ahora = LocalDateTime.now();
            EstadoPedido anterior = pedido.getEstado();
            if (anterior == EstadoPedido.EN_ALMACEN && !ahora.isBefore(pedido.getFechaPago().plusSeconds(30))) {
                pedido.setEstado(EstadoPedido.EN_CAMINO);
                pedido.setFechaSalida(ahora);
            } else if (anterior == EstadoPedido.EN_CAMINO && pedido.getFechaSalida() != null &&
                !ahora.isBefore(pedido.getFechaSalida().plusSeconds(60))) {
                pedido.setEstado(EstadoPedido.ENTREGADO);
                pedido.setFechaEntrega(ahora);
            }
            if (pedido.getEstado() != anterior) {
                pedido.setUsuarioActualizacion(actor.getEmail());
                pedido.setFechaActualizacion(ahora);
                if (!pedidoDAO.actualizarEstado(pedido, anterior))
                    throw new NegocioException("El estado del pedido cambio.");
                if (pedido.getEstado() == EstadoPedido.ENTREGADO)
                    premiarPrimeraEntrega(actor, ahora);
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo actualizar el seguimiento.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
        return consultar(idActor, idPedido);
    }

    private void premiarPrimeraEntrega(Usuario actor, LocalDateTime fecha) throws SQLException,
        NegocioException {
        ConfiguracionDTO parametros = new ConfiguracionBOImpl().obtener();
        if (!usuarioDAO.incrementarPedidosEntregados(actor.getId(), actor.getEmail()))
            throw new NegocioException("No se pudo contar la primera entrega.");
        int entregados = actor.getCantidadPedidosEntregados() + 1;
        if (entregados % parametros.pedidosEntregadosPorCupon() == 0) {
            Cupon cupon = new Cupon();
            cupon.setUsuario(actor);
            cupon.setPorcentaje(parametros.porcentajeCupon());
            cupon.setMontoMinimo(parametros.montoMinimoCupon());
            cupon.setFechaInicio(fecha);
            cupon.setFechaFin(fecha.plusDays(parametros.vigenciaDiasCupon()));
            cupon.setFechaCreacion(fecha);
            cupon.setUsuarioCreacion(actor.getEmail());
            cuponDAO.insertar(cupon);
        }
    }

    @Override
    public DetallePedidoDTO cancelar(int idActor, int idPedido, String motivo) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (motivo == null || motivo.isBlank())
                throw new ValidacionException("motivo", "Indica el motivo de cancelacion.");
            TransactionContext.getConnection();
            Pedido pedido = pedidoDAO.leer(idPedido);
            if (pedido == null || pedido.isEliminado())
                throw new RecursoNoEncontradoException("El pedido no existe.");
            if (pedido.getUsuario().getId() != idActor)
                throw new AccesoDenegadoException("El pedido pertenece a otra cuenta.");
            if (!pedido.puedeCancelar())
                throw new NegocioException("Solo puedes cancelar un pedido en almacen.");
            if (Math.random() >= 0.80)
                throw new NegocioException("La cancelacion simulada fue rechazada. El pedido se conserva.");
            LocalDateTime ahora = LocalDateTime.now();
            pedido.setEstado(EstadoPedido.CANCELADO);
            pedido.setUsuarioActualizacion(actor.getEmail());
            pedido.setFechaActualizacion(ahora);
            if (!pedidoDAO.actualizarEstado(pedido, EstadoPedido.EN_ALMACEN))
                throw new NegocioException("El pedido ya cambio de estado.");
            List<Item> items = pedido.getItems();
            for (Item item : items) if (!libroDAO.reponerStock(item.getLibro().getId(), item.getCantidad(),
                actor.getEmail()))
                throw new NegocioException("No se pudo reponer el stock del pedido.");
            if (pedido.getCupon() != null)
                cuponDAO.liberar(pedido.getCupon().getId(), ahora, actor.getEmail());
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo cancelar el pedido.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
        // El total del DTO permite anunciar el reembolso íntegro como mensaje temporal.
        return consultar(idActor, idPedido);
    }
}
