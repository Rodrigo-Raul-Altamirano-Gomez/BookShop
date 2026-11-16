package me.Business.recomendacion;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import me.Business.exceptions.*;
import me.DAO.catalogo.*;
import me.DAO.venta.PedidoDAO;
import me.Model.dto.catalogo.*;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.entities.catalogo.Libro;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecomendacionBOTest {

    LibroDAO libros = mock(LibroDAO.class);

    PedidoDAO pedidos = mock(PedidoDAO.class);

    ResenaDAO resenas = mock(ResenaDAO.class);

    List<Libro> disponibles = new ArrayList<>();

    Map<Integer, TarjetaLibroDTO> tarjetas = new HashMap<>();

    RecomendacionBO bo;

    @BeforeEach
    void preparar() throws Exception {
        for (int id = 1; id <= 4; id++) {
            disponibles.add(AlgoritmosRecomendacionTest.libro(id, 10));
            tarjetas.put(id, tarjeta(id, new BigDecimal(id * 5)));
        }
        when(libros.listarParaRecomendaciones()).thenAnswer(i -> new ArrayList<>(disponibles));
        when(libros.listarTarjetasPorIds(anyList(), any())).thenAnswer(i -> seleccionar(i.getArgument(0)));
        when(libros.buscar(any(FiltroLibroDTO.class), any())).thenAnswer(i -> {
            List<TarjetaLibroDTO> actuales = seleccionar(disponibles.stream().map(Libro::getId).toList());
            return new ResultadoPaginadoDTO<>(actuales, actuales.size(), 1, Integer.MAX_VALUE);
        });
        when(pedidos.listarDetalleVentas(any())).thenReturn(List.of());
        when(resenas.listarValoraciones()).thenReturn(List.of());
        bo = new RecomendacionBOImpl(libros, pedidos, resenas, Clock.fixed(AlgoritmosRecomendacionTest.AHORA.toInstant(ZoneOffset.ofHours(-5)), ZoneOffset.UTC));
    }

    List<TarjetaLibroDTO> seleccionar(List<Integer> ids) {
        return ids.stream().filter(tarjetas::containsKey).map(tarjetas::get).toList();
    }

    TarjetaLibroDTO tarjeta(int id, BigDecimal precio) {
        return new TarjetaLibroDTO(id, "Libro " + id, null, "Autor", precio, precio, BigDecimal.ZERO, true);
    }

    FiltroLibroDTO pagina(int numero, int tamanio) {
        return new FiltroLibroDTO(null, List.of(), List.of(), List.of(), List.of(), null, null,
            true, false, OrdenLibro.MAS_RECIENTES, numero, tamanio);
    }

    @Test
    void filtrosReducenElConjuntoConservandoOrdenYPaginacion() throws Exception {
        var abierta = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        var nuevos = tarjeta(99, BigDecimal.ONE);
        when(libros.buscar(any(FiltroLibroDTO.class), any())).thenReturn(
            new me.Model.dto.ResultadoPaginadoDTO<>(List.of(tarjetas.get(4), nuevos, tarjetas.get(2)), 3, 1, 100));
        var filtro = new FiltroLibroDTO("Libro", List.of(10), List.of(), List.of(), List.of(),
            BigDecimal.ONE, BigDecimal.TEN, true, false, OrdenLibro.PRECIO_DESCENDENTE, 1, 1);
        var pagina = bo.filtrar(0, abierta.idConsulta(), filtro);
        assertEquals(List.of(2), pagina.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(2, pagina.totalResultados());
        var captor = org.mockito.ArgumentCaptor.forClass(FiltroLibroDTO.class);
        verify(libros).buscar(captor.capture(), any());
        assertEquals("Libro", captor.getValue().titulo());
        assertEquals(List.of(10), captor.getValue().idsCategorias());
        assertEquals(BigDecimal.TEN, captor.getValue().precioMaximo());
        verify(pedidos, times(1)).listarDetalleVentas(any());
    }

    @Test
    void ordenaTodoAntesDePaginarYNoRepiteAlgoritmoAlCambiarPagina() throws Exception {
        var resultado = bo.abrirPopulares(new ConsultaRecomendacionesDTO(1, 2));
        assertEquals(List.of(1, 2), resultado.resultados().items().stream().map(TarjetaLibroDTO::id).toList());
        var segunda = bo.filtrar(0, resultado.idConsulta(), pagina(2, 2));
        assertEquals(List.of(3, 4), segunda.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(4, segunda.totalResultados());
        verify(pedidos, times(1)).listarDetalleVentas(any());
        verify(resenas, times(1)).listarValoraciones();
        verify(libros).listarTarjetasPorIds(eq(List.of(1, 2, 3, 4)), any());
        verify(libros).buscar(any(FiltroLibroDTO.class), any());
    }

    @Test
    void refrescaPreciosSinCambiarElOrdenNiAnadirNuevosLibros() throws Exception {
        var resultado = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        tarjetas.put(4, tarjeta(4, BigDecimal.ONE));
        disponibles.add(AlgoritmosRecomendacionTest.libro(5, 10));
        tarjetas.put(5, tarjeta(5, BigDecimal.ZERO));
        var pagina = bo.filtrar(0, resultado.idConsulta(), pagina(1, 12));
        assertEquals(List.of(1, 2, 3, 4), pagina.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(BigDecimal.ONE, pagina.items().getLast().precioConDescuento());
        assertEquals(4, pagina.totalResultados());
        verify(pedidos, times(1)).listarDetalleVentas(any());
    }

    @Test
    void filtrarYPaginarConservanElOrdenCalculadoAnteEmpates() throws Exception {
        when(pedidos.listarDetalleVentas(any())).thenReturn(List.of(
            AlgoritmosRecomendacionTest.venta(1, 1, 3, 5)));
        for (int id = 1; id <= 4; id++)
            tarjetas.put(id, tarjeta(id, BigDecimal.TEN));
        var resultado = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        assertEquals(List.of(3, 1, 2, 4), resultado.resultados().items().stream().map(TarjetaLibroDTO::id).toList());

        var primera = bo.filtrar(0, resultado.idConsulta(), pagina(1, 2));
        var segunda = bo.filtrar(0, resultado.idConsulta(), pagina(2, 2));
        assertEquals(List.of(3, 1), primera.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(List.of(2, 4), segunda.items().stream().map(TarjetaLibroDTO::id).toList());
        var recomendado = bo.filtrar(0, resultado.idConsulta(), FiltroLibroDTO.porDefecto());
        assertEquals(List.of(3, 1, 2, 4), recomendado.items().stream().map(TarjetaLibroDTO::id).toList());
        verify(pedidos, times(1)).listarDetalleVentas(any());
        verify(resenas, times(1)).listarValoraciones();
    }

    @Test
    void filtrarRefrescaAtributosSinCambiarElOrden() throws Exception {
        var resultado = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        Libro actualizado = disponibles.getLast();
        actualizado.setFechaCreacion(AlgoritmosRecomendacionTest.AHORA);
        actualizado.setNumeroPaginas(50);
        actualizado.setStockFisico(20);
        actualizado.setAnioEdicion(2026);
        tarjetas.put(4, new TarjetaLibroDTO(4, "A título actualizado", null, "Autor actualizado",
            BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("90"), true));

        var paginaActualizada = bo.filtrar(0, resultado.idConsulta(), pagina(1, 12));
        assertEquals(List.of(1, 2, 3, 4), paginaActualizada.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals("A título actualizado", paginaActualizada.items().getLast().titulo());
        assertEquals("Autor actualizado", paginaActualizada.items().getLast().nombreAutor());
        assertEquals(BigDecimal.ONE, paginaActualizada.items().getLast().precioConDescuento());
        verify(pedidos, times(1)).listarDetalleVentas(any());
    }

    @Test
    void conservaElConjuntoOriginalHastaReabrirYRetiraLibrosQueYaNoEstanDisponibles() throws Exception {
        var resultado = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        disponibles.removeIf(libro -> libro.getId() == 2);
        tarjetas.remove(2);
        disponibles.add(AlgoritmosRecomendacionTest.libro(5, 10));
        tarjetas.put(5, tarjeta(5, BigDecimal.ONE));

        var pagina = bo.filtrar(0, resultado.idConsulta(), FiltroLibroDTO.porDefecto());
        assertEquals(List.of(1, 3, 4), pagina.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(3, pagina.totalResultados());
        var nuevaConsulta = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        assertEquals(List.of(1, 3, 4, 5), nuevaConsulta.resultados().items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(4, nuevaConsulta.resultados().totalResultados());
    }

    @Test
    void paraTiCuentaPedidosDistintosYProtegeSuConsulta() throws Exception {
        var ventas = List.of(AlgoritmosRecomendacionTest.venta(1, 1, 1, 10), AlgoritmosRecomendacionTest.venta(2, 1, 1, 1), AlgoritmosRecomendacionTest.venta(3, 1, 1, 1), AlgoritmosRecomendacionTest.venta(4, 2, 1, 1), AlgoritmosRecomendacionTest.venta(4, 2, 2, 1));
        when(pedidos.listarDetalleVentas(any())).thenReturn(ventas);
        assertTrue(bo.puedeMostrarParaTi(1));
        var resultado = bo.abrirParaTi(1, ConsultaRecomendacionesDTO.porDefecto());
        assertFalse(resultado.resultados().items().stream().anyMatch(t -> t.id() == 1));
        assertThrows(AccesoDenegadoException.class, () -> bo.descartar(2, resultado.idConsulta()));
        assertThrows(AccesoDenegadoException.class, () -> bo.filtrar(2, resultado.idConsulta(), FiltroLibroDTO.porDefecto()));
        assertThrows(AccesoDenegadoException.class, () -> bo.filtrar(0, resultado.idConsulta(), FiltroLibroDTO.porDefecto()));
        bo.descartar(1, resultado.idConsulta());
        assertThrows(RecursoNoEncontradoException.class, () -> bo.filtrar(1, resultado.idConsulta(), FiltroLibroDTO.porDefecto()));
    }

    @Test
    void historialInsuficienteNoHacePasarEjemplaresPorPedidos() throws Exception {
        when(pedidos.listarDetalleVentas(any())).thenReturn(List.of(
            AlgoritmosRecomendacionTest.venta(1, 1, 1, 10),
            AlgoritmosRecomendacionTest.venta(1, 1, 2, 5),
            AlgoritmosRecomendacionTest.venta(2, 1, 3, 1),
            AlgoritmosRecomendacionTest.venta(3, 2, 1, 1)));
        assertFalse(bo.puedeMostrarParaTi(0));
        assertFalse(bo.puedeMostrarParaTi(1));
        assertThrows(ConflictoException.class, () -> bo.abrirParaTi(1, ConsultaRecomendacionesDTO.porDefecto()));
    }

    @Test
    void paraTiCompletaConPopularidadSinRepetirNiReintroducirComprados() throws Exception {
        for (int id = 5; id <= 6; id++) {
            disponibles.add(AlgoritmosRecomendacionTest.libro(id, 10));
            tarjetas.put(id, tarjeta(id, BigDecimal.TEN));
        }
        when(pedidos.listarDetalleVentas(any())).thenReturn(List.of(
            AlgoritmosRecomendacionTest.venta(1, 1, 1, 1),
            AlgoritmosRecomendacionTest.venta(2, 1, 2, 1),
            AlgoritmosRecomendacionTest.venta(3, 1, 3, 1),
            AlgoritmosRecomendacionTest.venta(4, 2, 1, 1),
            AlgoritmosRecomendacionTest.venta(4, 2, 2, 1),
            AlgoritmosRecomendacionTest.venta(4, 2, 4, 1),
            AlgoritmosRecomendacionTest.venta(5, 3, 5, 100)));
        var resultado = bo.abrirParaTi(1, ConsultaRecomendacionesDTO.porDefecto());
        assertEquals(List.of(4, 5, 6), resultado.resultados().items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(3, resultado.resultados().totalResultados());
    }

    @Test
    void paginasInvalidasSeRechazanYSoloSeExponeElMensajeDeNegocio() throws Exception {
        assertThrows(ValidacionException.class, () -> bo.abrirPopulares(new ConsultaRecomendacionesDTO(0, 12)));
        when(libros.listarParaRecomendaciones()).thenThrow(new SQLException("Detalle SQL interno"));
        var error = assertThrows(PersistenciaException.class, () -> bo.similares(1));
        assertFalse(error.getMessage().contains("Detalle SQL"));
    }

    @Test
    void similaresYComplementariosExcluyenElOrigen() throws Exception {
        assertFalse(bo.similares(1).stream().anyMatch(t -> t.id() == 1));
        assertFalse(bo.complementarios(List.of(1, 2)).stream().anyMatch(t -> t.id() == 1 || t.id() == 2));
        assertTrue(bo.complementarios(List.of()).isEmpty());
    }
}
