package me.Business.usuario;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.AutorDAO;
import me.DAO.catalogo.CategoriaDAO;
import me.DAO.catalogo.EditorialDAO;
import me.DAO.catalogo.LibroDAO;
import me.DAO.usuario.LibroGuardadoDAO;
import me.DAO.usuario.SeguimientoDAO;
import me.DAO.usuario.UsuarioDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.OrdenLibro;
import me.Model.dto.catalogo.OrdenReferencia;
import me.Model.dto.usuario.ConsultaSeguimientosDTO;
import me.Model.dto.usuario.TipoSeguimiento;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListasPersonalesBOTest {
    private Usuario actor;
    private UsuarioDAO usuarios;
    private LibroDAO libros;
    private LibroGuardadoDAO guardados;
    private SeguimientoDAO seguimientos;
    private AutorDAO autores;
    private CategoriaDAO categorias;
    private EditorialDAO editoriales;
    private LibroGuardadoBOImpl guardadosBO;
    private SeguimientoBOImpl seguimientosBO;

    @BeforeEach
    void preparar() throws Exception {
        actor = new Usuario();
        actor.setId(1);
        actor.setEmail("ana@test");
        usuarios = mock(UsuarioDAO.class);
        when(usuarios.leer(1)).thenReturn(actor);
        libros = mock(LibroDAO.class);
        guardados = mock(LibroGuardadoDAO.class);
        seguimientos = mock(SeguimientoDAO.class);
        autores = mock(AutorDAO.class);
        categorias = mock(CategoriaDAO.class);
        editoriales = mock(EditorialDAO.class);
        guardadosBO = new LibroGuardadoBOImpl(guardados, libros, usuarios,
                Clock.fixed(Instant.parse("2026-10-07T17:00:00Z"), ZoneId.of("UTC")));
        seguimientosBO = new SeguimientoBOImpl(seguimientos, autores, categorias, editoriales, usuarios);
    }

    @Test
    void guardadosSeparanDeseadoDeFavoritoYUnLibroSinStockPuedeGuardarse() throws Exception {
        Libro libro = new Libro();
        libro.setId(7);
        libro.setStockFisico(0);
        when(libros.leer(7)).thenReturn(libro);
        guardadosBO.guardar(1, 7, false);
        guardadosBO.guardar(1, 7, true);
        verify(guardados).guardar(1, 7, false, "ana@test");
        verify(guardados).guardar(1, 7, true, "ana@test");
        when(guardados.retirar(1, 7, false, "ana@test")).thenReturn(true, false);
        libro.setVetadoDirectamente(true);
        assertTrue(guardadosBO.retirar(1, 7, false));
        assertFalse(guardadosBO.retirar(1, 7, false));
        assertThrows(ConflictoException.class, () -> guardadosBO.guardar(1, 7, false));
        verify(guardados, never()).retirar(1, 7, true, "ana@test");
    }

    @Test
    void listadoConservaAmbitoPropioYValidaTodoElFiltroAntesDeConsultar() throws Exception {
        FiltroLibroDTO filtro = new FiltroLibroDTO(" Titulo ", List.of(2), List.of(), List.of(), List.of(),
                new BigDecimal("10.00"), new BigDecimal("20.00"), false, false,
                OrdenLibro.PRECIO_ASCENDENTE, 2, 12);
        when(libros.buscarGuardados(eq(1), eq(true), any(), any()))
                .thenReturn(new ResultadoPaginadoDTO<>(List.of(), 0, 2, 12));
        guardadosBO.listar(1, true, filtro);
        ArgumentCaptor<FiltroLibroDTO> captor = ArgumentCaptor.forClass(FiltroLibroDTO.class);
        verify(libros).buscarGuardados(eq(1), eq(true), captor.capture(), any());
        assertTrue(captor.getValue().soloActivos());
        assertEquals("Titulo", captor.getValue().titulo());
        assertEquals(List.of(2), captor.getValue().idsCategorias());
        assertEquals(2, captor.getValue().pagina());
        FiltroLibroDTO invalido = new FiltroLibroDTO(null, List.of(), List.of(), List.of(), List.of(),
                new BigDecimal("20"), new BigDecimal("10"), true, false, OrdenLibro.MAS_RECIENTES, 1, 12);
        assertThrows(ValidacionException.class, () -> guardadosBO.listar(1, true, invalido));
        verify(libros, times(1)).buscarGuardados(anyInt(), anyBoolean(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(TipoSeguimiento.class)
    void seguimientoUsaElSubtipoElegidoSinPermitirListasAjenas(TipoSeguimiento tipo) throws Exception {
        when(autores.leer(5)).thenReturn(new Autor());
        when(categorias.leer(5)).thenReturn(new Categoria());
        when(editoriales.leer(5)).thenReturn(new Editorial());
        seguimientosBO.guardar(1, tipo, 5);
        verify(seguimientos).guardar(1, tipo, 5, "ana@test");
        when(seguimientos.retirar(1, tipo, 5, "ana@test")).thenReturn(true, false);
        assertTrue(seguimientosBO.retirar(1, tipo, 5));
        assertFalse(seguimientosBO.retirar(1, tipo, 5));
        when(seguimientos.listarPorUsuario(1, tipo, OrdenReferencia.MAS_RECIENTES, 1, 12))
                .thenReturn(new ResultadoPaginadoDTO<>(List.of(), 0, 1, 12));
        seguimientosBO.listar(1, new ConsultaSeguimientosDTO(tipo, OrdenReferencia.MAS_RECIENTES, 1, 12));
        verify(seguimientos).listarPorUsuario(1, tipo, OrdenReferencia.MAS_RECIENTES, 1, 12);
        verify(seguimientos, never()).listarPorUsuario(eq(2), any(), any(), anyInt(), anyInt());
    }

    @Test
    void referenciaInactivaRechazaSeguirPeroPermiteRetirarRelacionPropia() throws Exception {
        Autor inactivo = new Autor();
        inactivo.setActivo(false);
        when(autores.leer(5)).thenReturn(inactivo);
        assertThrows(ConflictoException.class, () -> seguimientosBO.guardar(1, TipoSeguimiento.AUTOR, 5));
        verify(seguimientos, never()).guardar(anyInt(), any(), anyInt(), anyString());
        when(seguimientos.retirar(1, TipoSeguimiento.AUTOR, 5, "ana@test")).thenReturn(true);
        assertTrue(seguimientosBO.retirar(1, TipoSeguimiento.AUTOR, 5));
    }

    @Test
    void autenticacionYCuentaActivaSeCompruebanAntesDeLeerOWriteListas() throws Exception {
        assertThrows(AccesoDenegadoException.class, () -> guardadosBO.guardar(0, 7, true));
        verifyNoInteractions(libros, guardados);
        actor.setActivo(false);
        assertThrows(CuentaInactivaException.class, () -> guardadosBO.listar(1, true, new FiltroLibroDTO()));
        assertThrows(CuentaInactivaException.class,
                () -> seguimientosBO.listar(1, new ConsultaSeguimientosDTO()));
        verifyNoInteractions(libros, guardados, seguimientos);
    }
}
