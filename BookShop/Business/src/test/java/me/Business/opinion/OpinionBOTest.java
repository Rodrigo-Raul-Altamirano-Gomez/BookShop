package me.Business.opinion;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.ResenaDAO;
import me.DAO.catalogo.VotoUtilDAO;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.venta.PedidoDAO;
import me.DBManager.TransactionContext;
import me.Model.dto.opinion.ConsultaResenasDTO;
import me.Model.dto.opinion.DatosResenaDTO;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.opinion.Resena;
import me.Model.entities.usuario.Rol;
import me.Model.entities.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OpinionBOTest {
    private final ResenaDAO resenas = mock(ResenaDAO.class);
    private final LibroDAO libros = mock(LibroDAO.class);
    private final PedidoDAO pedidos = mock(PedidoDAO.class);
    private final VotoUtilDAO votos = mock(VotoUtilDAO.class);
    private final UsuarioDAO usuarios = mock(UsuarioDAO.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-07T04:30:00Z"), ZoneOffset.UTC);
    private ResenaBOImpl bo;
    private Resena resena;
    private Usuario usuario;

    @BeforeEach
    void preparar() throws Exception {
        usuario = new Usuario(); usuario.setId(1); usuario.setEmail("cliente@test"); usuario.setRol(Rol.CLIENTE);
        Usuario ajeno = new Usuario(); ajeno.setId(2); ajeno.setEmail("otro@test");
        Usuario admin = new Usuario(); admin.setId(3); admin.setEmail("admin@admin.bookshop"); admin.setRol(Rol.ADMIN);
        when(usuarios.leer(1)).thenReturn(usuario);
        when(usuarios.leer(2)).thenReturn(ajeno);
        when(usuarios.leer(3)).thenReturn(admin);
        Libro libro = new Libro(); libro.setId(5);
        when(libros.leer(5)).thenReturn(libro);
        when(pedidos.haCompradoLibro(1, 5)).thenReturn(true);
        when(pedidos.haCompradoLibroParaActualizar(1, 5)).thenReturn(true);
        resena = new Resena(); resena.setId(7); resena.setLibro(libro); resena.setUsuario(usuario);
        resena.setPuntuacion(4); resena.setComentario("Anterior");
        when(resenas.leer(7)).thenReturn(resena);
        when(resenas.leerParaActualizar(7)).thenReturn(resena);
        bo = new ResenaBOImpl(resenas, libros, pedidos, usuarios, reloj);
    }

    @Test
    void verificaElegibilidadAntesDelFormularioSinEscribir() throws Exception {
        bo.verificarPuedePublicar(1, 5);
        verify(resenas, never()).insertar(any());
        verify(resenas, never()).insertarEnTransaccion(any());
        when(pedidos.haCompradoLibro(1, 5)).thenReturn(false);
        assertThrows(AccesoDenegadoException.class, () -> bo.verificarPuedePublicar(1, 5));
    }

    @Test
    void unaActivaProduceConflictoYNoSeRecuperaUnaEliminada() throws Exception {
        when(resenas.existePublicada(1, 5)).thenReturn(true);
        assertThrows(ConflictoException.class, () -> bo.publicar(1, 5, new DatosResenaDTO(5, "Texto")));
        verify(resenas, never()).insertarEnTransaccion(any());
        when(resenas.existePublicada(1, 5)).thenReturn(false);
        doAnswer(llamada -> { ((Resena) llamada.getArgument(0)).setId(8); return null; }).when(resenas).insertarEnTransaccion(any());
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertEquals(8, bo.publicar(1, 5, new DatosResenaDTO(5, " Nueva ")));
            ArgumentCaptor<Resena> captor = ArgumentCaptor.forClass(Resena.class);
            verify(resenas).insertarEnTransaccion(captor.capture());
            assertEquals("Nueva", captor.getValue().getComentario());
            assertEquals(LocalDateTime.of(2026, 10, 6, 23, 30), captor.getValue().getFecha());
            verify(resenas, never()).modificar(any());
            contexto.verify(TransactionContext::commit);
            contexto.verify(TransactionContext::close);
        }
    }

    @Test
    void compraCanceladaMientrasSeGuardaImpidePublicacionYRevierte() throws Exception {
        when(pedidos.haCompradoLibroParaActualizar(1, 5)).thenReturn(false);
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertThrows(ConflictoException.class, () -> bo.publicar(1, 5, new DatosResenaDTO(3, "Texto")));
            verify(resenas, never()).insertarEnTransaccion(any());
            contexto.verify(TransactionContext::rollback);
            contexto.verify(TransactionContext::commit, never());
            contexto.verify(TransactionContext::close);
        }
    }

    @Test
    void falloAlInsertarPublicacionRevierteYConservaCausa() throws Exception {
        SQLException fallo = new SQLException("Conexion perdida", "08001");
        doThrow(fallo).when(resenas).insertarEnTransaccion(any());
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            PersistenciaException error = assertThrows(PersistenciaException.class, () -> bo.publicar(1, 5, new DatosResenaDTO(3, "Texto")));
            assertSame(fallo, error.getCause());
            contexto.verify(TransactionContext::rollback);
            contexto.verify(TransactionContext::commit, never());
        }
    }

    @Test
    void edicionPropiaNoVuelveAExigirCompraDespuesDeCancelacion() throws Exception {
        bo.modificar(1, 7, new DatosResenaDTO(2, "Editado"));
        verify(resenas).modificar(resena);
        verifyNoInteractions(pedidos);
        assertEquals("Editado", resena.getComentario());
        assertEquals("cliente@test", resena.getUsuarioActualizacion());
    }

    @Test
    void noPuedeEditarAjenaNiAdministradorUsarEdicionParaAjena() throws Exception {
        assertThrows(AccesoDenegadoException.class, () -> bo.modificar(2, 7, new DatosResenaDTO(5, "Ajena")));
        assertThrows(AccesoDenegadoException.class, () -> bo.modificar(3, 7, new DatosResenaDTO(5, "Admin")));
        verify(resenas, never()).modificar(any());
    }

    @Test
    void retiradaAdministrativaNoEstaDisponibleAlCliente() throws Exception {
        assertThrows(AccesoDenegadoException.class, () -> bo.eliminarAdministracion(1, 7));
        when(resenas.eliminar(7, "admin@admin.bookshop")).thenReturn(true);
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            bo.eliminarAdministracion(3, 7);
            verify(resenas).eliminar(7, "admin@admin.bookshop");
            contexto.verify(TransactionContext::commit);
        }
    }

    @Test
    void listasPropiasPublicasYAdministrativasConservanAmbito() throws Exception {
        bo.listarPorLibro(0, 5, new ConsultaResenasDTO());
        bo.listarPropias(1, new ConsultaResenasDTO());
        bo.listarAdministracion(3, new ConsultaResenasDTO());
        verify(resenas).listar(eq(5), eq(0), eq(0), any());
        verify(resenas).listar(eq(0), eq(1), eq(1), any());
        verify(resenas).listar(eq(0), eq(0), eq(3), any());
        assertThrows(AccesoDenegadoException.class, () -> bo.listarAdministracion(1, new ConsultaResenasDTO()));
    }

    @Test
    void camposInvalidosNoCreanPublicacion() throws Exception {
        assertThrows(ValidacionException.class, () -> bo.publicar(1, 5, new DatosResenaDTO(0, "Texto")));
        assertThrows(ValidacionException.class, () -> bo.publicar(1, 5, new DatosResenaDTO(5, "  ")));
        verify(resenas, never()).insertarEnTransaccion(any());
        verifyNoInteractions(pedidos);
    }

    @Test
    void votosRepetidosDevuelvenEstadoIdempotenteYNoConfirmanFallo() throws Exception {
        VotoUtilBOImpl votosBO = new VotoUtilBOImpl(votos, resenas, libros, usuarios);
        when(votos.cambiarVoto(1, 7, true, "cliente@test")).thenReturn(true, false);
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertTrue(votosBO.cambiarVoto(1, 7, true));
            assertFalse(votosBO.cambiarVoto(1, 7, true));
            contexto.verify(TransactionContext::commit, times(2));
        }
        doThrow(new SQLException("Fallo contador")).when(votos).cambiarVoto(1, 7, false, "cliente@test");
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            assertThrows(PersistenciaException.class, () -> votosBO.cambiarVoto(1, 7, false));
            contexto.verify(TransactionContext::rollback);
            contexto.verify(TransactionContext::commit, never());
        }
    }

    @Test
    void cuentaInactivaOResenaRetiradaImpidenVoto() throws Exception {
        VotoUtilBOImpl votosBO = new VotoUtilBOImpl(votos, resenas, libros, usuarios);
        usuario.setActivo(false);
        assertThrows(CuentaInactivaException.class, () -> votosBO.cambiarVoto(1, 7, true));
        usuario.setActivo(true);
        resena.setEliminado(true);
        assertThrows(RecursoNoEncontradoException.class, () -> votosBO.cambiarVoto(1, 7, true));
        verifyNoInteractions(votos);
    }
}
