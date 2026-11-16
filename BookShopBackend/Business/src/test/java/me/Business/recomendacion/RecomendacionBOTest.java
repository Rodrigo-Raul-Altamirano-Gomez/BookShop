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

    @Test
    void ordenaTodoAntesDePaginarYNoRepiteAlgoritmoAlCambiarPagina() throws Exception {
        var resultado = bo.abrirPopulares(new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_DESCENDENTE, 1, 2));
        assertEquals(List.of(4, 3), resultado.resultados().items().stream().map(TarjetaLibroDTO::id).toList());
        var segunda = bo.consultar(0, resultado.idConsulta(), new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_DESCENDENTE, 2, 2));
        assertEquals(List.of(2, 1), segunda.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(4, segunda.totalResultados());
        verify(pedidos, times(1)).listarDetalleVentas(any());
        verify(resenas, times(1)).listarValoraciones();
        // Cada consulta obtiene el conjunto una vez y reutiliza las tarjetas al paginar.
        verify(libros, times(2)).listarTarjetasPorIds(eq(List.of(1, 2, 3, 4)), any());
    }

    @Test
    void refrescaPreciosDeTodoElConjuntoAntesDeOrdenarSinAnadirNuevosLibros() throws Exception {
        var resultado = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        tarjetas.put(4, tarjeta(4, BigDecimal.ONE));
        disponibles.add(AlgoritmosRecomendacionTest.libro(5, 10));
        tarjetas.put(5, tarjeta(5, BigDecimal.ZERO));
        var pagina = bo.consultar(0, resultado.idConsulta(), new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_ASCENDENTE, 1, 2));
        assertEquals(List.of(4, 1), pagina.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(4, pagina.totalResultados());
        verify(pedidos, times(1)).listarDetalleVentas(any());
    }

    @Test
    void empatesConservanElOrdenCalculadoAunqueAntesSeHayaUsadoOtroOrden() throws Exception {
        when(pedidos.listarDetalleVentas(any())).thenReturn(List.of(
            AlgoritmosRecomendacionTest.venta(1, 1, 3, 5)));
        for (int id = 1; id <= 4; id++)
            tarjetas.put(id, tarjeta(id, BigDecimal.TEN));
        var resultado = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        assertEquals(List.of(3, 1, 2, 4), resultado.resultados().items().stream().map(TarjetaLibroDTO::id).toList());

        var alfabetico = bo.consultar(0, resultado.idConsulta(),
            new ConsultaRecomendacionesDTO(OrdenLibro.TITULO_ASCENDENTE, 1, 12));
        assertEquals(List.of(1, 2, 3, 4), alfabetico.items().stream().map(TarjetaLibroDTO::id).toList());
        var primera = bo.consultar(0, resultado.idConsulta(),
            new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_ASCENDENTE, 1, 2));
        var segunda = bo.consultar(0, resultado.idConsulta(),
            new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_ASCENDENTE, 2, 2));
        assertEquals(List.of(3, 1), primera.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(List.of(2, 4), segunda.items().stream().map(TarjetaLibroDTO::id).toList());
        var recomendado = bo.consultar(0, resultado.idConsulta(), ConsultaRecomendacionesDTO.porDefecto());
        assertEquals(List.of(3, 1, 2, 4), recomendado.items().stream().map(TarjetaLibroDTO::id).toList());
        verify(pedidos, times(1)).listarDetalleVentas(any());
        verify(resenas, times(1)).listarValoraciones();
    }

    @Test
    void consultarUsaAtributosActualesParaOrdenar() throws Exception {
        var resultado = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        Libro actualizado = disponibles.getLast();
        actualizado.setFechaCreacion(AlgoritmosRecomendacionTest.AHORA);
        actualizado.setNumeroPaginas(50);
        actualizado.setStockFisico(20);
        actualizado.setAnioEdicion(2026);
        tarjetas.put(4, new TarjetaLibroDTO(4, "A título actualizado", null, "Autor actualizado",
            BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("90"), true));

        for (OrdenLibro orden : List.of(OrdenLibro.MAS_RECIENTES, OrdenLibro.PAGINAS_ASCENDENTE,
            OrdenLibro.STOCK_DESCENDENTE, OrdenLibro.ANIO_EDICION_DESCENDENTE, OrdenLibro.TITULO_ASCENDENTE)) {
            var pagina = bo.consultar(0, resultado.idConsulta(), new ConsultaRecomendacionesDTO(orden, 1, 12));
            assertEquals(4, pagina.items().getFirst().id(), orden.toString());
            assertEquals("A título actualizado", pagina.items().getFirst().titulo());
            assertEquals("Autor actualizado", pagina.items().getFirst().nombreAutor());
            assertEquals(BigDecimal.ONE, pagina.items().getFirst().precioConDescuento());
        }
        var masPaginas = bo.consultar(0, resultado.idConsulta(),
            new ConsultaRecomendacionesDTO(OrdenLibro.PAGINAS_DESCENDENTE, 1, 12));
        assertEquals(List.of(1, 2, 3, 4), masPaginas.items().stream().map(TarjetaLibroDTO::id).toList());
        verify(pedidos, times(1)).listarDetalleVentas(any());
    }

    @Test
    void conservaElConjuntoOriginalHastaReabrirYRetiraLibrosQueYaNoEstanDisponibles() throws Exception {
        var resultado = bo.abrirPopulares(ConsultaRecomendacionesDTO.porDefecto());
        disponibles.removeIf(libro -> libro.getId() == 2);
        tarjetas.remove(2);
        disponibles.add(AlgoritmosRecomendacionTest.libro(5, 10));
        tarjetas.put(5, tarjeta(5, BigDecimal.ONE));

        var pagina = bo.consultar(0, resultado.idConsulta(), ConsultaRecomendacionesDTO.porDefecto());
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
        assertThrows(AccesoDenegadoException.class, () -> bo.consultar(2, resultado.idConsulta(), ConsultaRecomendacionesDTO.porDefecto()));
        assertThrows(AccesoDenegadoException.class, () -> bo.descartar(2, resultado.idConsulta()));
        assertThrows(AccesoDenegadoException.class, () -> bo.consultar(0, resultado.idConsulta(), ConsultaRecomendacionesDTO.porDefecto()));
        bo.descartar(1, resultado.idConsulta());
        assertThrows(RecursoNoEncontradoException.class, () -> bo.consultar(1, resultado.idConsulta(), ConsultaRecomendacionesDTO.porDefecto()));
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
    void vistasNoPermitenMasVendidosNiExponenSql() throws Exception {
        assertThrows(ValidacionException.class, () -> bo.abrirPopulares(new ConsultaRecomendacionesDTO(OrdenLibro.MAS_VENDIDOS, 1, 12)));
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
