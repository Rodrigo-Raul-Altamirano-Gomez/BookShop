package me.Business.recomendacion;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import me.Business.exceptions.*;
import me.DAO.catalogo.*;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.venta.PedidoDAO;
import me.Model.dto.catalogo.*;
import me.Model.dto.compra.VentaLibroDTO;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.Usuario;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecomendacionBOTest {
    LibroDAO libros = mock(LibroDAO.class);
    PedidoDAO pedidos = mock(PedidoDAO.class);
    ResenaDAO resenas = mock(ResenaDAO.class);
    UsuarioDAO usuarios = mock(UsuarioDAO.class);
    List<Libro> disponibles = new ArrayList<>();
    Map<Integer,TarjetaLibroDTO> tarjetas = new HashMap<>();
    RecomendacionBO bo;
    @BeforeEach void preparar() throws Exception {
        for (int id=1;id<=4;id++) {
            disponibles.add(AlgoritmosRecomendacionTest.libro(id,10));
            tarjetas.put(id, tarjeta(id,new BigDecimal(id*5)));
        }
        when(libros.listarParaRecomendaciones()).thenAnswer(i -> new ArrayList<>(disponibles));
        when(libros.listarTarjetasSinImagenPorIds(anyList(),any())).thenAnswer(i -> seleccionar(i.getArgument(0)));
        when(libros.listarTarjetasPorIds(anyList(),any())).thenAnswer(i -> seleccionar(i.getArgument(0)));
        when(pedidos.listarDetalleVentas(any())).thenReturn(List.of());
        when(resenas.listarValoraciones()).thenReturn(List.of());
        for (int id=1;id<=2;id++) { Usuario u = new Usuario(); u.setId(id); when(usuarios.leer(id)).thenReturn(u); }
        bo = new RecomendacionBOImpl(libros,pedidos,resenas,usuarios,
                Clock.fixed(AlgoritmosRecomendacionTest.AHORA.toInstant(ZoneOffset.ofHours(-5)), ZoneOffset.UTC));
    }
    List<TarjetaLibroDTO> seleccionar(List<Integer> ids) {
        return ids.stream().filter(tarjetas::containsKey).map(tarjetas::get).toList();
    }
    TarjetaLibroDTO tarjeta(int id, BigDecimal precio) {
        return new TarjetaLibroDTO(id,"Libro "+id,null,"Autor",precio,precio,BigDecimal.ZERO,true);
    }
    @Test void ordenaTodoAntesDePaginarYNoRepiteAlgoritmoAlCambiarPagina() throws Exception {
        var resultado = bo.abrirPopulares(new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_DESCENDENTE,1,2));
        assertEquals(List.of(4,3), resultado.resultados().items().stream().map(TarjetaLibroDTO::id).toList());
        var segunda = bo.consultar(0,resultado.idConsulta(),new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_DESCENDENTE,2,2));
        assertEquals(List.of(2,1), segunda.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(4,segunda.totalResultados());
        verify(pedidos,times(1)).listarDetalleVentas(any());
        verify(resenas,times(1)).listarValoraciones();
        verify(libros).listarTarjetasPorIds(eq(List.of(4,3)),any());
        verify(libros).listarTarjetasPorIds(eq(List.of(2,1)),any());
    }
    @Test void refrescaPreciosDeTodoElConjuntoAntesDeOrdenarSinAnadirNuevosLibros() throws Exception {
        var resultado = bo.abrirPopulares(new ConsultaRecomendacionesDTO());
        tarjetas.put(4,tarjeta(4,BigDecimal.ONE));
        disponibles.add(AlgoritmosRecomendacionTest.libro(5,10));
        tarjetas.put(5,tarjeta(5,BigDecimal.ZERO));
        var pagina = bo.consultar(0,resultado.idConsulta(),new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_ASCENDENTE,1,2));
        assertEquals(List.of(4,1),pagina.items().stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(4,pagina.totalResultados());
        verify(pedidos,times(1)).listarDetalleVentas(any());
    }
    @Test void paraTiCuentaPedidosDistintosYProtegeSuConsulta() throws Exception {
        var ventas = List.of(AlgoritmosRecomendacionTest.venta(1,1,1,10),AlgoritmosRecomendacionTest.venta(2,1,1,1),
                AlgoritmosRecomendacionTest.venta(3,1,1,1),AlgoritmosRecomendacionTest.venta(4,2,1,1),
                AlgoritmosRecomendacionTest.venta(4,2,2,1));
        when(pedidos.listarDetalleVentas(any())).thenReturn(ventas);
        assertTrue(bo.puedeMostrarParaTi(1));
        var resultado = bo.abrirParaTi(1,new ConsultaRecomendacionesDTO());
        assertFalse(resultado.resultados().items().stream().anyMatch(t -> t.id()==1));
        assertThrows(AccesoDenegadoException.class, () -> bo.consultar(2,resultado.idConsulta(),new ConsultaRecomendacionesDTO()));
        bo.descartar(1,resultado.idConsulta());
        assertThrows(RecursoNoEncontradoException.class, () -> bo.consultar(1,resultado.idConsulta(),new ConsultaRecomendacionesDTO()));
    }
    @Test void historialInsuficienteNoHacePasarEjemplaresPorPedidos() throws Exception {
        when(pedidos.listarDetalleVentas(any())).thenReturn(List.of(AlgoritmosRecomendacionTest.venta(1,1,1,10)));
        assertFalse(bo.puedeMostrarParaTi(0));
        assertFalse(bo.puedeMostrarParaTi(1));
        assertThrows(ConflictoException.class, () -> bo.abrirParaTi(1,new ConsultaRecomendacionesDTO()));
    }
    @Test void vistasNoPermitenMasVendidosNiExponenSql() throws Exception {
        assertThrows(ValidacionException.class, () -> bo.abrirPopulares(new ConsultaRecomendacionesDTO(OrdenLibro.MAS_VENDIDOS,1,12)));
        when(libros.listarParaRecomendaciones()).thenThrow(new SQLException("Detalle SQL interno"));
        var error = assertThrows(PersistenciaException.class, () -> bo.similares(1));
        assertFalse(error.getMessage().contains("Detalle SQL"));
    }
    @Test void similaresYComplementariosExcluyenElOrigen() throws Exception {
        assertFalse(bo.similares(1).stream().anyMatch(t -> t.id()==1));
        assertFalse(bo.complementarios(List.of(1,2)).stream().anyMatch(t -> t.id()==1 || t.id()==2));
        assertTrue(bo.complementarios(List.of()).isEmpty());
    }
}
