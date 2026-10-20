package me.Business.solicitud;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import me.Business.compra.PedidoBOImpl;
import me.Business.compra.ProveedorConfiguracion;
import me.Business.exceptions.*;
import me.DAO.catalogo.*;
import me.DAO.solicitud.SolicitudDAO;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.venta.*;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ConfiguracionDTO;
import me.Model.dto.compra.ResumenPedidoDTO;
import me.Model.dto.opinion.DatosResenaDTO;
import me.Model.dto.solicitud.*;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.compra.*;
import me.Model.entities.opinion.Resena;
import me.Model.entities.solicitud.*;
import me.Model.entities.usuario.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SolicitudesBusinessTest {
    private final LocalDateTime fecha = LocalDateTime.of(2026, 10, 7, 10, 0);
    private final ConfiguracionDTO parametros = new ConfiguracionDTO(new BigDecimal("5"), 30, 3, 5,
            new BigDecimal("10"), new BigDecimal("100"), 30);
    private SolicitudDAO solicitudes;
    private PedidoDAO pedidos;
    private ResenaDAO resenas;
    private LibroDAO libros;
    private CuponDAO cupones;
    private UsuarioDAO usuarios;
    private Usuario cliente;
    private Usuario admin;
    private Pedido pedido;
    private Resena resena;
    private SolicitudBOImpl negocio;
    private AtomicReference<Solicitud> registro;
    private Connection conexion;
    private MockedStatic<DBManager> manager;
    private Clock reloj;

    @BeforeEach
    void preparar() throws Exception {
        solicitudes = mock(SolicitudDAO.class);
        pedidos = mock(PedidoDAO.class);
        resenas = mock(ResenaDAO.class);
        libros = mock(LibroDAO.class);
        cupones = mock(CuponDAO.class);
        usuarios = mock(UsuarioDAO.class);
        conexion = mock(Connection.class);
        manager = mockStatic(DBManager.class);
        manager.when(DBManager::getConnection).thenReturn(conexion);
        reloj = Clock.fixed(fecha.atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        cliente = new Usuario(); cliente.setId(1); cliente.setEmail("ana@test");
        admin = new Usuario(); admin.setId(9); admin.setEmail("admin@admin.bookshop"); admin.setRol(Rol.ADMIN);
        when(usuarios.leer(1)).thenReturn(cliente);
        when(usuarios.leer(9)).thenReturn(admin);
        pedido = new Pedido();
        pedido.setId(10);
        pedido.setUsuario(cliente);
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setFechaEntrega(fecha.minusDays(30));
        pedido.setFechaPago(fecha.minusDays(31));
        pedido.setTotal(new BigDecimal("63.00"));
        pedido.setCantidadTotalLibros(2);
        Libro libro = new Libro(); libro.setId(3);
        Item item = new Item(0, libro, 2); item.setPrecioBase(new BigDecimal("50.00")); item.setPrecioUnitario(new BigDecimal("30.00"));
        pedido.setItems(List.of(item));
        Cupon cupon = new Cupon(); cupon.setId(5); pedido.setCupon(cupon);
        when(pedidos.leer(10)).thenReturn(pedido);
        when(pedidos.leerParaActualizar(10)).thenReturn(pedido);
        when(pedidos.incrementarIntentos(eq(10), anyInt(), eq("ana@test"))).thenReturn(true);
        when(pedidos.actualizarEstado(any(), any())).thenReturn(true);
        when(libros.reponerStock(anyInt(), anyInt(), anyString())).thenReturn(true);
        when(cupones.liberar(anyInt(), any(), anyString())).thenReturn(true);
        resena = new Resena(4, 2, "Texto original", libro, cliente);
        when(resenas.leer(4)).thenReturn(resena);
        when(resenas.leerParaActualizar(4)).thenReturn(resena);
        when(resenas.eliminar(4, "admin@admin.bookshop")).thenReturn(true);
        registro = new AtomicReference<>();
        doAnswer(inv -> {
            Solicitud nueva = inv.getArgument(0);
            nueva.setId(7);
            registro.set(nueva);
            return null;
        }).when(solicitudes).insertar(any());
        when(solicitudes.leer(7)).thenAnswer(inv -> registro.get());
        when(solicitudes.leerParaActualizar(7)).thenAnswer(inv -> registro.get());
        when(solicitudes.resolver(eq(7), any(), any(), eq("admin@admin.bookshop"))).thenAnswer(inv -> {
            registro.get().resolver(inv.getArgument(1), inv.getArgument(3), inv.getArgument(2));
            return true;
        });
        when(solicitudes.obtenerDetalleDevolucion(7)).thenAnswer(inv -> {
            SolicitudDevolucion solicitud = (SolicitudDevolucion) registro.get();
            return new DetalleDevolucionDTO(resumen(solicitud), new DatosDevolucionDTO(solicitud.getMotivo(), solicitud.getComentario()),
                    new ResumenPedidoDTO(10, pedido.getFechaPago(), pedido.getEstado(), 2, pedido.getTotal()), solicitud.getFechaResolucion());
        });
        when(solicitudes.obtenerDetalleReporteResena(eq(7), anyBoolean())).thenAnswer(inv -> {
            SolicitudReporteResena solicitud = (SolicitudReporteResena) registro.get();
            boolean actuales = inv.getArgument(1);
            return new DetalleReporteResenaDTO(resumen(solicitud), new DatosReporteResenaDTO(solicitud.getMotivo(), solicitud.getComentario()),
                    new ResenaReportadaDTO(4, "Ana", "Libro", new DatosResenaDTO(solicitud.getPuntuacionOriginal(), solicitud.getComentarioOriginal())),
                    actuales ? new DatosResenaDTO(resena.getPuntuacion(), resena.getComentario()) : null, resena.estaDisponible(), solicitud.getFechaResolucion());
        });
        negocio = construir(() -> parametros);
    }

    private SolicitudBOImpl construir(ProveedorConfiguracion configuracion) {
        return new SolicitudBOImpl(solicitudes, pedidos, resenas, libros, cupones, usuarios, reloj,
                configuracion, mock(PedidoBOImpl.class));
    }

    private ResumenSolicitudDTO resumen(Solicitud solicitud) {
        TipoSolicitud tipo = solicitud instanceof SolicitudDevolucion ? TipoSolicitud.DEVOLUCION : TipoSolicitud.REPORTE_RESENA;
        return new ResumenSolicitudDTO(solicitud.getId(), tipo, solicitud.getFechaCreacion(), solicitud.getEstado(), "Ana", "Motivo", tipo == TipoSolicitud.DEVOLUCION ? 10 : 4);
    }

    @AfterEach
    void cerrar() {
        TransactionContext.close();
        if (manager != null) manager.close();
    }

    @Test
    void formularioInvalidoNoAbreTransaccionNiConsumeIntento() throws Exception {
        assertThrows(ValidacionException.class, () -> negocio.enviarDevolucion(1, 10, new DatosDevolucionDTO(null, "Comentario")));
        verifyNoInteractions(conexion);
        verify(pedidos, never()).leerParaActualizar(anyInt());
        verify(pedidos, never()).incrementarIntentos(anyInt(), anyInt(), anyString());
        verify(solicitudes, never()).insertar(any());
    }

    @Test
    void ultimoInstanteDelPlazoAdmiteSolicitudYMantienePedidoEntregado() throws Exception {
        var resultado = negocio.enviarDevolucion(1, 10, new DatosDevolucionDTO(MotivoDevolucion.LIBRO_DANADO, "  Portada dañada  "));
        assertEquals(EstadoSolicitud.PENDIENTE, resultado.solicitud().estado());
        assertEquals(EstadoPedido.ENTREGADO, resultado.pedido().estado());
        assertEquals("Portada dañada", resultado.datos().comentario());
        var orden = inOrder(pedidos, solicitudes, conexion);
        orden.verify(pedidos).leerParaActualizar(10);
        orden.verify(pedidos).incrementarIntentos(10, 3, "ana@test");
        orden.verify(solicitudes).insertar(any());
        orden.verify(conexion).commit();
        verify(pedidos, never()).actualizarEstado(any(), any());
        verify(libros, never()).reponerStock(anyInt(), anyInt(), anyString());
    }

    @Test
    void fueraPlazoPendienteYLimiteNoConsumenOtroIntento() throws Exception {
        pedido.setFechaEntrega(fecha.minusDays(30).minusNanos(1));
        assertThrows(FueraPlazoException.class, () -> negocio.enviarDevolucion(1, 10, new DatosDevolucionDTO(MotivoDevolucion.OTRO, null)));
        pedido.setFechaEntrega(fecha.minusDays(30));
        when(solicitudes.existeDevolucionPendiente(10)).thenReturn(true);
        assertThrows(ConflictoException.class, () -> negocio.enviarDevolucion(1, 10, new DatosDevolucionDTO(MotivoDevolucion.OTRO, null)));
        when(solicitudes.existeDevolucionPendiente(10)).thenReturn(false);
        pedido.setNumeroIntentos(3);
        assertThrows(LimiteIntentosException.class, () -> negocio.enviarDevolucion(1, 10, new DatosDevolucionDTO(MotivoDevolucion.OTRO, null)));
        verify(pedidos, never()).incrementarIntentos(anyInt(), anyInt(), anyString());
        verify(solicitudes, never()).insertar(any());
        verify(conexion, never()).commit();
    }

    @Test
    void falloAlRegistrarRevierteIntentoYNoAnunciaDevolucion() throws Exception {
        doThrow(new SQLException("Fallo en solicitud")).when(solicitudes).insertar(any());
        assertThrows(PersistenciaException.class, () -> negocio.enviarDevolucion(1, 10, new DatosDevolucionDTO(MotivoDevolucion.LIBRO_DANADO, null)));
        verify(pedidos).incrementarIntentos(10, 3, "ana@test");
        verify(conexion).rollback();
        verify(conexion, never()).commit();
        verify(libros, never()).reponerStock(anyInt(), anyInt(), anyString());
        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
    }

    @Test
    void resolverPendienteNoRevalidaNuevoPlazoConfigYReponeUnaSolaVez() throws Exception {
        SolicitudDevolucion solicitud = new SolicitudDevolucion(7, cliente, pedido, MotivoDevolucion.LIBRO_DANADO, null);
        solicitud.setFechaCreacion(fecha.minusDays(29));
        registro.set(solicitud);
        pedido.setFechaEntrega(fecha.minusDays(60));
        negocio = construir(() -> { throw new NegocioException("No debe leerse configuracion al resolver pendiente"); });
        var resuelta = negocio.resolverDevolucion(9, 7, EstadoSolicitud.ACEPTADA);
        assertEquals(EstadoSolicitud.ACEPTADA, resuelta.solicitud().estado());
        assertEquals(EstadoPedido.DEVUELTO, resuelta.pedido().estado());
        assertEquals(new BigDecimal("63.00"), resuelta.pedido().total());
        verify(libros).reponerStock(3, 2, "admin@admin.bookshop");
        verify(cupones).liberar(5, fecha, "admin@admin.bookshop");
        verify(usuarios, never()).incrementarPedidosEntregados(anyInt(), anyString());
        assertThrows(ConflictoException.class, () -> negocio.resolverDevolucion(9, 7, EstadoSolicitud.ACEPTADA));
        verify(libros, times(1)).reponerStock(3, 2, "admin@admin.bookshop");
    }

    @Test
    void editarReporteConservaOriginalYNoExponeActualACliente() throws Exception {
        negocio.enviarReporte(1, 4, new DatosReporteResenaDTO(MotivoReporteResena.CONTENIDO_OFENSIVO, null));
        SolicitudReporteResena reporte = (SolicitudReporteResena) registro.get();
        resena.setComentario("Texto corregido");
        resena.setPuntuacion(5);
        var editada = negocio.editarReporte(1, 7, new DatosReporteResenaDTO(MotivoReporteResena.OTRO, "Aclaracion"));
        assertEquals("Texto original", reporte.getComentarioOriginal());
        assertEquals(2, reporte.getPuntuacionOriginal());
        assertNull(editada.contenidoActual());
        assertEquals("Texto original", editada.resenaReportada().contenidoOriginal().comentario());
        verify(pedidos, never()).incrementarIntentos(anyInt(), anyInt(), anyString());
        var administrativa = negocio.consultarReporte(9, 7);
        assertEquals("Texto corregido", administrativa.contenidoActual().comentario());
        assertThrows(AccesoDenegadoException.class, () -> negocio.resolverReporte(1, 7, EstadoSolicitud.ACEPTADA));
    }

    @Test
    void moderacionAceptadaEsAtomicaYFalloAlRetirarRevierteResolucion() throws Exception {
        SolicitudReporteResena reporte = new SolicitudReporteResena(7, cliente, resena, MotivoReporteResena.CONTENIDO_OFENSIVO, null);
        registro.set(reporte);
        when(resenas.eliminar(4, "admin@admin.bookshop")).thenThrow(new SQLException("Fallo de moderacion"));
        assertThrows(PersistenciaException.class, () -> negocio.resolverReporte(9, 7, EstadoSolicitud.ACEPTADA));
        var orden = inOrder(solicitudes, resenas, conexion);
        orden.verify(solicitudes).leerParaActualizar(7);
        orden.verify(resenas).leerParaActualizar(4);
        orden.verify(solicitudes).resolver(7, EstadoSolicitud.ACEPTADA, fecha, "admin@admin.bookshop");
        orden.verify(resenas).eliminar(4, "admin@admin.bookshop");
        orden.verify(conexion).rollback();
        verify(conexion, never()).commit();
    }
}
