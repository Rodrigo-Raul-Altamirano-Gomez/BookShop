package me.Business.compra;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import me.Business.exceptions.*;
import me.Business.ProteccionDatos;
import me.DAO.catalogo.*;
import me.DAO.solicitud.SolicitudDAO;
import me.DAO.usuario.*;
import me.DAO.venta.*;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ConfiguracionDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.*;
import me.Model.entities.catalogo.*;
import me.Model.entities.compra.*;
import me.Model.entities.usuario.Usuario;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CompraBusinessTest {
    private final LocalDateTime fecha = LocalDateTime.of(2026, 10, 7, 10, 0);
    private final ConfiguracionDTO parametros = new ConfiguracionDTO(new BigDecimal("5"), 30, 3, 5,
            new BigDecimal("10"), new BigDecimal("100"), 30);
    private LibroDAO libros;
    private DescuentoDAO descuentos;
    private UsuarioDAO usuarios;
    private PedidoDAO pedidos;
    private DireccionEnvioDAO direcciones;
    private MetodoPagoDAO metodos;
    private CuponDAO cupones;
    private SolicitudDAO solicitudes;
    private CarritoBOImpl carrito;
    private PedidoBOImpl negocio;
    private Libro libro;
    private Usuario usuario;
    private DireccionEnvio direccion;
    private TarjetaBancaria metodo;
    private AtomicReference<Pedido> registrado;
    private AtomicReference<Double> azar;
    private Reloj reloj;
    private Connection conexion;
    private MockedStatic<DBManager> manager;
    private MockedStatic<ProteccionDatos> proteccion;

    @BeforeEach
    void preparar() throws Exception {
        libros = mock(LibroDAO.class);
        descuentos = mock(DescuentoDAO.class);
        usuarios = mock(UsuarioDAO.class);
        pedidos = mock(PedidoDAO.class);
        direcciones = mock(DireccionEnvioDAO.class);
        metodos = mock(MetodoPagoDAO.class);
        cupones = mock(CuponDAO.class);
        solicitudes = mock(SolicitudDAO.class);
        conexion = mock(Connection.class);
        manager = mockStatic(DBManager.class);
        manager.when(DBManager::getConnection).thenReturn(conexion);
        proteccion = mockStatic(ProteccionDatos.class);
        proteccion.when(() -> ProteccionDatos.descifrar("cifrado")).thenReturn("4111111111111111");
        proteccion.when(() -> ProteccionDatos.enmascarar("4111111111111111")).thenReturn("************1111");
        reloj = new Reloj(fecha);
        azar = new AtomicReference<>(0.0);
        registrado = new AtomicReference<>();
        usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("ana@test");
        usuario.setCantidadPedidosEntregados(4);
        when(usuarios.leer(1)).thenReturn(usuario);
        when(usuarios.leerParaActualizar(1)).thenReturn(usuario);
        when(usuarios.incrementarPedidosEntregados(1, "ana@test")).thenReturn(true);
        libro = new Libro();
        libro.setId(3);
        libro.setTitulo("Libro con centavos");
        libro.setPrecioBase(new BigDecimal("19.99"));
        libro.setStockFisico(20);
        when(libros.leer(3)).thenReturn(libro);
        when(libros.leerParaActualizar(3)).thenReturn(libro);
        when(libros.descontarStock(eq(3), anyInt(), eq("ana@test"))).thenReturn(true);
        when(libros.reponerStock(eq(3), anyInt(), anyString())).thenReturn(true);
        Descuento promocion = new Descuento();
        promocion.setPorcentaje(new BigDecimal("15"));
        when(descuentos.obtenerVigente(eq(3), any())).thenReturn(promocion);
        direccion = new DireccionEnvio();
        direccion.setId(1);
        direccion.setUsuario(usuario);
        direccion.setAlias("Casa");
        direccion.setCalle("Calle 1");
        direccion.setDistrito("Distrito");
        direccion.setCiudad("Lima");
        direccion.setPredeterminada(true);
        when(direcciones.leer(1)).thenReturn(direccion);
        when(direcciones.leerParaActualizar(1)).thenReturn(direccion);
        when(direcciones.listarPorUsuario(1, 1, 100)).thenReturn(new ResultadoPaginadoDTO<>(List.of(
                new DireccionEnvioDTO(1, "Casa", "Calle 1", "Distrito", "Lima", null, true)), 1, 1, 100));
        metodo = new TarjetaBancaria();
        metodo.setId(1);
        metodo.setUsuario(usuario);
        metodo.setTitular("Ana");
        metodo.setProveedor("Banco");
        metodo.setNumeroTarjetaCifrado("cifrado");
        metodo.setTipoTarjeta(TipoTarjeta.CREDITO);
        metodo.setFechaVencimiento(LocalDate.of(2030, 1, 1));
        metodo.setPredeterminado(true);
        when(metodos.leer(1)).thenReturn(metodo);
        when(metodos.leerParaActualizar(1)).thenReturn(metodo);
        when(metodos.listarPorUsuario(1, 1, 100)).thenReturn(new ResultadoPaginadoDTO<>(List.of(metodo), 1, 1, 100));
        doAnswer(inv -> {
            Pedido pedido = inv.getArgument(0);
            pedido.setId(20);
            registrado.set(pedido);
            return null;
        }).when(pedidos).insertar(any());
        when(pedidos.leer(20)).thenAnswer(inv -> registrado.get());
        when(pedidos.leerParaActualizar(20)).thenAnswer(inv -> registrado.get());
        when(pedidos.actualizarEstado(any(), any())).thenReturn(true);
        when(pedidos.obtenerItemsDTO(20)).thenAnswer(inv -> registrado.get().getItems().stream()
                .map(item -> new ItemDTO(item.getLibro().getId(), libro.getTitulo(), null, item.getCantidad(),
                        item.getPrecioBase(), item.getPrecioUnitario(), item.getPorcentajeDescuento(), item.getSubTotal(), null, null)).toList());
        carrito = new CarritoBOImpl(libros, descuentos, usuarios, reloj, () -> parametros);
        negocio = new PedidoBOImpl(pedidos, libros, descuentos, usuarios, direcciones, metodos, cupones,
                solicitudes, carrito, reloj, () -> azar.get(), () -> parametros);
    }

    @AfterEach
    void cerrar() {
        TransactionContext.close();
        if (proteccion != null) proteccion.close();
        if (manager != null) manager.close();
    }

    @Test
    void navegarNoAbreTransaccionYCalculaDineroConRedondeoExacto() throws Exception {
        var resumen = carrito.agregar(1, 3, 2);
        assertEquals(new BigDecimal("33.98"), resumen.subtotal());
        assertEquals(new BigDecimal("1.70"), resumen.costoEnvio());
        assertEquals(new BigDecimal("35.68"), resumen.total());
        var preparacion = negocio.preparar(1);
        assertTrue(preparacion.puedePagar());
        assertEquals(15, preparacion.resumen().items().getFirst().porcentajeDescuento().intValueExact());
        assertThrows(StockInsuficienteException.class, () -> carrito.cambiarCantidad(1, 3, 99));
        assertEquals(2, carrito.consultar(1).cantidadLibros());
        verifyNoInteractions(conexion);
        verify(pedidos, never()).insertar(any());
    }

    @Test
    void cantidadTotalNoDesbordaYNoModificaUnaCompraPreparada() throws Exception {
        libro.setPrecioBase(new BigDecimal("0.01"));
        libro.setStockFisico(Integer.MAX_VALUE);
        when(descuentos.obtenerVigente(eq(3), any())).thenReturn(null);
        carrito.agregar(1, 3, Integer.MAX_VALUE);
        var preparacion = negocio.preparar(1);
        assertThrows(ValidacionException.class, () -> carrito.agregar(1, 3, 1));
        assertThrows(ValidacionException.class, () -> carrito.agregar(1, 4, 1));
        assertEquals(Integer.MAX_VALUE, carrito.consultar(1).cantidadLibros());
        assertSame(preparacion, negocio.consultarPreparacion(1));
        verify(libros, never()).leer(4);
        verifyNoInteractions(conexion);
    }

    @Test
    void importeIncluidoEnvioDebeCaberEnPedidoAntesDeModificarCarrito() throws Exception {
        libro.setPrecioBase(new BigDecimal("9999999999.99"));
        libro.setStockFisico(1000);
        when(descuentos.obtenerVigente(eq(3), any())).thenReturn(null);
        carrito.agregar(1, 3, 95);
        var preparacion = negocio.preparar(1);
        // 96 ejemplares caben en subtotal, pero su total con envio excede DECIMAL(14,2).
        assertThrows(ValidacionException.class, () -> carrito.agregar(1, 3, 1));
        assertThrows(ValidacionException.class, () -> carrito.cambiarCantidad(1, 3, 101));
        assertEquals(95, carrito.consultar(1).cantidadLibros());
        assertSame(preparacion, negocio.consultarPreparacion(1));
        libro.setPrecioBase(new BigDecimal("10000000000.00"));
        assertThrows(ValidacionException.class, () -> carrito.cambiarCantidad(1, 3, 1));
        assertSame(preparacion, negocio.consultarPreparacion(1));
        verifyNoInteractions(conexion);
    }

    @Test
    void relojInyectadoUtcUsaFechaLimaParaVencimientoYPago() throws Exception {
        Clock utc = Clock.fixed(Instant.parse("2026-10-08T01:00:00Z"), ZoneOffset.UTC);
        LocalDateTime ahoraLima = LocalDateTime.of(2026, 10, 7, 20, 0);
        metodo.setFechaVencimiento(ahoraLima.toLocalDate());
        carrito = new CarritoBOImpl(libros, descuentos, usuarios, utc, () -> parametros);
        negocio = new PedidoBOImpl(pedidos, libros, descuentos, usuarios, direcciones, metodos, cupones,
                solicitudes, carrito, utc, () -> azar.get(), () -> parametros);
        carrito.agregar(1, 3, 1);
        assertTrue(negocio.preparar(1).puedePagar());
        assertTrue(negocio.pagar(1).metodoPago().vigente());
        assertEquals(ahoraLima, registrado.get().getFechaPago());
        verify(descuentos, atLeastOnce()).obtenerVigente(3, ahoraLima);
    }

    @Test
    void cuponIncluyeMinimoExactoYSeleccionInvalidaConservaLaAnterior() throws Exception {
        libro.setPrecioBase(new BigDecimal("100.00"));
        when(descuentos.obtenerVigente(eq(3), any())).thenReturn(null);
        carrito.agregar(1, 3, 1);
        negocio.preparar(1);
        Cupon cupon = new Cupon();
        cupon.setId(5);
        cupon.setUsuario(usuario);
        cupon.setPorcentaje(new BigDecimal("10"));
        cupon.setMontoMinimo(new BigDecimal("100.00"));
        cupon.setFechaInicio(fecha.minusDays(1));
        cupon.setFechaFin(fecha.plusDays(1));
        when(cupones.leer(5)).thenReturn(cupon);
        var seleccionada = negocio.seleccionar(1, new SeleccionCompraDTO(1, 1, 5));
        assertEquals(new BigDecimal("10.00"), seleccionada.resumen().descuentoCupon());
        assertEquals(new BigDecimal("5.00"), seleccionada.resumen().costoEnvio());
        assertEquals(new BigDecimal("95.00"), seleccionada.resumen().total());
        assertThrows(ConflictoException.class, () -> negocio.seleccionar(1, new SeleccionCompraDTO(1, 1, 99)));
        assertSame(seleccionada, negocio.consultarPreparacion(1));
        verifyNoInteractions(conexion);
    }

    @Test
    void rechazoNoConsumeStockNiCuponYConservaCarritoYSelecciones() throws Exception {
        carrito.agregar(1, 3, 2);
        var preparacion = negocio.preparar(1);
        azar.set(0.90);
        assertThrows(PagoRechazadoException.class, () -> negocio.pagar(1));
        assertEquals(preparacion.seleccion(), negocio.consultarPreparacion(1).seleccion());
        assertEquals(2, carrito.consultar(1).cantidadLibros());
        verify(libros, never()).descontarStock(anyInt(), anyInt(), anyString());
        verify(cupones, never()).consumir(anyInt(), anyInt(), any(), anyString());
        verify(pedidos, never()).insertar(any());
        verify(conexion).rollback();
        verify(conexion, never()).commit();
    }

    @Test
    void cambioMaterialSeComunicaAntesDeSimularCobroYPermiteRevisar() throws Exception {
        carrito.agregar(1, 3, 2);
        negocio.preparar(1);
        libro.setPrecioBase(new BigDecimal("29.99"));
        assertThrows(CondicionesCompraCambiaronException.class, () -> negocio.pagar(1));
        assertEquals(new BigDecimal("25.49"), negocio.consultarPreparacion(1).resumen().items().getFirst().precioUnitario());
        verify(pedidos, never()).insertar(any());
        verify(libros, never()).descontarStock(anyInt(), anyInt(), anyString());
        var confirmado = negocio.pagar(1);
        assertEquals(20, confirmado.pedido().id());
        assertEquals(0, carrito.consultar(1).cantidadLibros());
        assertEquals("************1111", confirmado.metodoPago().numeroEnmascarado());
        assertEquals(new BigDecimal("15"), registrado.get().getItems().getFirst().getPorcentajeDescuento());
    }

    @Test
    void falloDeDatosDespuesDeCommitNoPermiteCobrarDeNuevo() throws Exception {
        carrito.agregar(1, 3, 2);
        negocio.preparar(1);
        when(pedidos.leer(20)).thenThrow(new SQLException("Fallo al construir la respuesta"))
                .thenAnswer(inv -> registrado.get());
        assertThrows(PersistenciaException.class, () -> negocio.pagar(1));
        var repetido = negocio.pagar(1);
        assertEquals(20, repetido.pedido().id());
        verify(pedidos, times(1)).insertar(any());
        verify(libros, times(1)).descontarStock(3, 2, "ana@test");
        verify(conexion, times(1)).commit();
        verify(conexion, never()).rollback();
    }

    @Test
    void falloAlInsertarRevierteTransaccionSinVaciarCarrito() throws Exception {
        carrito.agregar(1, 3, 2);
        negocio.preparar(1);
        doThrow(new SQLException("Fallo de items")).when(pedidos).insertar(any());
        assertThrows(PersistenciaException.class, () -> negocio.pagar(1));
        verify(libros).descontarStock(3, 2, "ana@test");
        verify(conexion).rollback();
        verify(conexion, never()).commit();
        assertEquals(2, carrito.consultar(1).cantidadLibros());
    }

    @Test
    void seguimientoAvanzaUnaEtapaEnLimitesYSoloPremiaPrimeraEntrega() throws Exception {
        carrito.agregar(1, 3, 2);
        negocio.preparar(1);
        negocio.pagar(1);
        reloj.avanzar(29);
        assertEquals(EstadoPedido.EN_ALMACEN, negocio.actualizarSeguimiento(1, 20).pedido().estado());
        reloj.avanzar(1);
        assertEquals(EstadoPedido.EN_CAMINO, negocio.actualizarSeguimiento(1, 20).pedido().estado());
        assertEquals(fecha.plusSeconds(30), registrado.get().getFechaSalida());
        reloj.avanzar(59);
        assertEquals(EstadoPedido.EN_CAMINO, negocio.actualizarSeguimiento(1, 20).pedido().estado());
        reloj.avanzar(1);
        assertEquals(EstadoPedido.ENTREGADO, negocio.actualizarSeguimiento(1, 20).pedido().estado());
        assertEquals(fecha.plusSeconds(90), registrado.get().getFechaEntrega());
        reloj.avanzar(500);
        negocio.actualizarSeguimiento(1, 20);
        verify(usuarios, times(1)).incrementarPedidosEntregados(1, "ana@test");
        var premio = org.mockito.ArgumentCaptor.forClass(Cupon.class);
        verify(cupones, times(1)).insertar(premio.capture());
        assertEquals(fecha.plusSeconds(90).plusDays(30), premio.getValue().getFechaFin());
        verify(pedidos, times(2)).actualizarEstado(any(), any());
    }

    @Test
    void cancelacionRechazadaNoReponeYExitoDevuelveTotalIncluidoEnvio() throws Exception {
        carrito.agregar(1, 3, 2);
        negocio.preparar(1);
        negocio.pagar(1);
        azar.set(0.80);
        assertThrows(ConflictoException.class, () -> negocio.cancelar(1, 20, "Cambie de idea"));
        verify(libros, never()).reponerStock(anyInt(), anyInt(), anyString());
        assertEquals(EstadoPedido.EN_ALMACEN, registrado.get().getEstado());
        azar.set(0.0);
        var cancelado = negocio.cancelar(1, 20, "Cambie de idea");
        assertEquals(new BigDecimal("35.68"), cancelado.pedido().total());
        assertEquals(EstadoPedido.CANCELADO, cancelado.pedido().estado());
        assertNull(cancelado.codigoSeguimiento());
        assertNull(cancelado.fechaEntregaEstimada());
        assertFalse(cancelado.puedeCancelar());
        assertThrows(ConflictoException.class, () -> negocio.cancelar(1, 20, "Repetir"));
        verify(libros, times(1)).reponerStock(3, 2, "ana@test");
    }

    static class Reloj extends Clock {
        private Instant instante;
        private final ZoneId zona = ZoneId.of("America/Lima");
        Reloj(LocalDateTime fecha) { instante = fecha.atZone(zona).toInstant(); }
        void avanzar(long segundos) { instante = instante.plusSeconds(segundos); }
        @Override public ZoneId getZone() { return zona; }
        @Override public Clock withZone(ZoneId zone) {
            if (zona.equals(zone)) return this;
            throw new IllegalArgumentException("El reloj de prueba conserva Lima.");
        }
        @Override public Instant instant() { return instante; }
    }
}
