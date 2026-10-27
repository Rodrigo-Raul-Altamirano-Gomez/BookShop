package me.Business.catalogo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.AutorDAO;
import me.DAO.catalogo.CategoriaDAO;
import me.DAO.catalogo.DescuentoDAO;
import me.DAO.catalogo.EditorialDAO;
import me.DAO.catalogo.LibroDAO;
import me.DAO.usuario.UsuarioDAO;
import me.DBManager.TransactionContext;
import me.Model.dto.catalogo.DatosDescuentoDTO;
import me.Model.dto.catalogo.DatosLibroDTO;
import me.Model.dto.catalogo.DatosReferenciaDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.OrdenLibro;
import me.Model.dto.catalogo.RegistroLibroDTO;
import me.Model.dto.catalogo.RegistroReferenciaDTO;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.Rol;
import me.Model.entities.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogoBOTest {
    private final LibroDAO libros = mock(LibroDAO.class);
    private final AutorDAO autores = mock(AutorDAO.class);
    private final CategoriaDAO categorias = mock(CategoriaDAO.class);
    private final EditorialDAO editoriales = mock(EditorialDAO.class);
    private final UsuarioDAO usuarios = mock(UsuarioDAO.class);
    private final DescuentoDAO descuentos = mock(DescuentoDAO.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-07T04:30:00Z"), ZoneOffset.UTC);
    private LibroBOImpl bo;
    private Libro libro;
    private Autor autor;
    private Categoria categoria;
    private Editorial editorial;

    @BeforeEach
    void preparar() throws Exception {
        Usuario admin = new Usuario();
        admin.setId(1); admin.setEmail("admin@admin.bookshop"); admin.setRol(Rol.ADMIN);
        Usuario cliente = new Usuario();
        cliente.setId(2); cliente.setEmail("cliente@test"); cliente.setRol(Rol.CLIENTE);
        when(usuarios.leer(1)).thenReturn(admin);
        when(usuarios.leer(2)).thenReturn(cliente);
        autor = new Autor(); autor.setId(1);
        categoria = new Categoria(); categoria.setId(1);
        editorial = new Editorial(); editorial.setId(1);
        libro = new Libro(1, "Titulo", autor, editorial, categoria, new BigDecimal("20.00"), 5);
        when(libros.leer(1)).thenReturn(libro);
        when(libros.leerParaActualizar(1)).thenReturn(libro);
        when(autores.leerParaActualizar(1)).thenReturn(autor);
        when(categorias.leerParaActualizar(1)).thenReturn(categoria);
        when(editoriales.leerParaActualizar(1)).thenReturn(editorial);
        bo = new LibroBOImpl(libros, autores, categorias, editoriales, usuarios, reloj);
    }

    private DatosLibroDTO datos() {
        return new DatosLibroDTO(" Nuevo ", " Español ", " Sinopsis ", " Proveedor ", " ISBN ", 2026, 100,
                null, null, null, null, null);
    }

    private RegistroLibroDTO registro(DatosLibroDTO datos) {
        return new RegistroLibroDTO(datos, 1, 1, 1, new BigDecimal("19.99"), 10, null);
    }

    @Test
    void clienteNoPuedeGestionarYNoEscribe() throws Exception {
        assertThrows(AccesoDenegadoException.class, () -> bo.registrar(2, registro(datos())));
        assertThrows(AccesoDenegadoException.class, () -> bo.cambiarPrecio(2, 1, BigDecimal.TEN));
        verifyNoInteractions(libros, autores, categorias, editoriales);
    }

    @Test
    void registrarNormalizaGuardaAuditoriaLimaYConfirmaConReferenciasBloqueadas() throws Exception {
        doAnswer(llamada -> { ((Libro) llamada.getArgument(0)).setId(15); return null; }).when(libros).insertar(any());
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertEquals(15, bo.registrar(1, registro(datos())));
            ArgumentCaptor<Libro> captor = ArgumentCaptor.forClass(Libro.class);
            verify(libros).insertar(captor.capture());
            assertEquals("Nuevo", captor.getValue().getTitulo());
            assertEquals("ISBN", captor.getValue().getIsbn());
            assertEquals(LocalDateTime.of(2026, 10, 6, 23, 30), captor.getValue().getFechaCreacion());
            assertEquals("admin@admin.bookshop", captor.getValue().getUsuarioCreacion());
            contexto.verify(TransactionContext::commit);
            contexto.verify(TransactionContext::close);
            contexto.verify(TransactionContext::rollback, never());
        }
    }

    @Test
    void referenciaQueSeVetoAntesDeGuardarImpideAltaYRevierte() throws Exception {
        autor.setActivo(false);
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertThrows(ConflictoException.class, () -> bo.registrar(1, registro(datos())));
            verify(libros, never()).insertar(any());
            contexto.verify(TransactionContext::rollback);
            contexto.verify(TransactionContext::close);
            contexto.verify(TransactionContext::commit, never());
        }
    }

    @Test
    void falloJdbcDelAltaSeTraduceYRevierte() throws Exception {
        SQLException fallo = new SQLException("Fallo de conexion", "08001");
        doThrow(fallo).when(libros).insertar(any());
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            PersistenciaException error = assertThrows(PersistenciaException.class, () -> bo.registrar(1, registro(datos())));
            assertSame(fallo, error.getCause());
            contexto.verify(TransactionContext::rollback);
            contexto.verify(TransactionContext::close);
        }
    }

    @Test
    void isbnReservadoInclusoSiFueRetiradoImpideAlta() throws Exception {
        when(libros.existeIsbn("ISBN", 0)).thenReturn(true);
        assertThrows(ValidacionException.class, () -> bo.registrar(1, registro(datos())));
        verify(libros, never()).insertar(any());
    }

    @Test
    void editarDatosConservaReferenciasPrecioStockImagen() throws Exception {
        libro.setPortada(new byte[]{1});
        bo.modificar(1, 1, datos());
        verify(libros).modificar(libro);
        assertSame(autor, libro.getAutor());
        assertSame(categoria, libro.getCategoria());
        assertSame(editorial, libro.getEditorial());
        assertEquals(new BigDecimal("20.00"), libro.getPrecioBase());
        assertEquals(5, libro.getStockFisico());
        assertArrayEquals(new byte[]{1}, libro.getPortada());
        assertEquals("admin@admin.bookshop", libro.getUsuarioActualizacion());
    }

    @Test
    void validacionesDePrecisionMedidasTomoYStockOcurrenAntesDeEscribir() throws Exception {
        assertThrows(ValidacionException.class, () -> bo.cambiarPrecio(1, 1, new BigDecimal("1.001")));
        assertThrows(ValidacionException.class, () -> bo.cambiarStock(1, 1, -1));
        DatosLibroDTO invalidos = new DatosLibroDTO("Titulo", "Idioma", "Sinopsis", "Proveedor", "ISBN", 2026, 100,
                Double.NaN, null, null, null, null);
        assertThrows(ValidacionException.class, () -> bo.registrar(1, registro(invalidos)));
        DatosLibroDTO tomo = new DatosLibroDTO("Titulo", "Idioma", "Sinopsis", "Proveedor", "ISBN", 2026, 100,
                null, null, null, null, 1);
        assertThrows(ValidacionException.class, () -> bo.registrar(1, registro(tomo)));
        verifyNoInteractions(libros);
    }

    @Test
    void activarLibroNoEliminaVetoAjeno() throws Exception {
        libro.setNumeroVetos(2); libro.setVetadoDirectamente(true);
        assertThrows(ConflictoException.class, () -> bo.cambiarActivo(1, 1, true));
        verify(libros, never()).cambiarVetoDirecto(anyInt(), anyBoolean(), anyString());
    }

    @Test
    void filtrosNormalizanDuplicadosYProtegenListadoAdministrativo() throws Exception {
        FiltroLibroDTO filtro = new FiltroLibroDTO(" Titulo ", List.of(1, 1), List.of(2), null,
                List.of(" Español ", "Español"), null, null, true, false, OrdenLibro.MAS_RECIENTES, 2, 12);
        bo.buscar(0, filtro);
        ArgumentCaptor<FiltroLibroDTO> captor = ArgumentCaptor.forClass(FiltroLibroDTO.class);
        verify(libros).buscar(captor.capture(), eq(LocalDateTime.of(2026, 10, 6, 23, 30)));
        assertEquals(List.of(1), captor.getValue().idsCategorias());
        assertEquals(List.of("Español"), captor.getValue().idiomas());
        assertEquals("Titulo", captor.getValue().titulo());
        assertThrows(AccesoDenegadoException.class, () -> bo.buscar(2, new FiltroLibroDTO(null, null, null, null, null,
                null, null, false, false, OrdenLibro.MAS_RECIENTES, 1, 12)));
    }

    @Test
    void compararExigeDosLibrosDistintosSinGuardarSeleccion() {
        assertThrows(ValidacionException.class, () -> bo.comparar(0, 1, 1));
        verifyNoInteractions(libros);
    }

    @Test
    void categoriaNoDuplicaNombreReservado() throws Exception {
        when(categorias.existeNombre("Categoria", 0)).thenReturn(true);
        CategoriaBOImpl categoriasBO = new CategoriaBOImpl(categorias, usuarios, reloj);
        assertThrows(ValidacionException.class, () -> categoriasBO.registrar(1, new RegistroReferenciaDTO(new DatosReferenciaDTO(" Categoria ", "texto"), null)));
        verify(categorias, never()).insertar(any());
    }

    @Test
    void vetoReferenciaConfirmaEfectosSoloUnaVezYFalloRevierte() throws Exception {
        when(autores.cambiarActivo(1, false, "admin@admin.bookshop")).thenReturn(true);
        AutorBOImpl autoresBO = new AutorBOImpl(autores, usuarios, reloj);
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            autoresBO.cambiarActivo(1, 1, false);
            contexto.verify(TransactionContext::commit);
            contexto.verify(TransactionContext::close);
        }
        doThrow(new SQLException("Fallo")).when(autores).cambiarActivo(1, false, "admin@admin.bookshop");
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertThrows(PersistenciaException.class, () -> autoresBO.cambiarActivo(1, 1, false));
            contexto.verify(TransactionContext::rollback);
            contexto.verify(TransactionContext::commit, never());
        }
    }

    @Test
    void descuentosEnLimiteSonVigentesYSeConfirmaLaOperacion() throws Exception {
        DescuentoBOImpl descuentosBO = new DescuentoBOImpl(descuentos, libros, usuarios, reloj);
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 6, 23, 30);
        doAnswer(llamada -> { ((Descuento) llamada.getArgument(0)).setId(9); return null; }).when(descuentos).insertar(any());
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertEquals(9, descuentosBO.guardar(1, 1, new DatosDescuentoDTO(0, new BigDecimal("100"), ahora,
                    ahora.plusDays(1), false)));
            ArgumentCaptor<Descuento> captor = ArgumentCaptor.forClass(Descuento.class);
            verify(descuentos).insertar(captor.capture());
            assertTrue(captor.getValue().isActivo());
            assertEquals(new BigDecimal("100.00"), captor.getValue().getPorcentaje());
            contexto.verify(TransactionContext::commit);
        }
    }

    @Test
    void checkboxEliminarEsAplicadoAlGuardarSinValidarCamposDescartados() throws Exception {
        Descuento previo = new Descuento(7, libro, BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        when(descuentos.leerParaActualizar(7)).thenReturn(previo);
        when(descuentos.eliminar(7, "admin@admin.bookshop")).thenReturn(true);
        DescuentoBOImpl descuentosBO = new DescuentoBOImpl(descuentos, libros, usuarios, reloj);
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertEquals(7, descuentosBO.guardar(1, 1, new DatosDescuentoDTO(7, null, null, null, true)));
            verify(descuentos).eliminar(7, "admin@admin.bookshop");
            verify(descuentos, never()).modificar(any());
            contexto.verify(TransactionContext::commit);
        }
    }

    @Test
    void solapamientoODescuentoDeOtroLibroNoModificaPersistencia() throws Exception {
        DescuentoBOImpl descuentosBO = new DescuentoBOImpl(descuentos, libros, usuarios, reloj);
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 6, 23, 30);
        when(descuentos.haySuperposicion(1, 0, inicio, inicio.plusDays(1))).thenReturn(true);
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertThrows(ValidacionException.class, () -> descuentosBO.guardar(1, 1,
                    new DatosDescuentoDTO(0, BigDecimal.TEN, inicio, inicio.plusDays(1), false)));
            contexto.verify(TransactionContext::rollback);
        }
        Libro otro = new Libro(); otro.setId(99);
        when(descuentos.leerParaActualizar(7)).thenReturn(new Descuento(7, otro, BigDecimal.TEN, inicio, inicio.plusDays(1)));
        assertThrows(AccesoDenegadoException.class, () -> descuentosBO.guardar(1, 1,
                new DatosDescuentoDTO(7, BigDecimal.TEN, inicio, inicio.plusDays(1), false)));
        verify(descuentos, never()).insertar(any());
        verify(descuentos, never()).modificar(any());
    }
}
