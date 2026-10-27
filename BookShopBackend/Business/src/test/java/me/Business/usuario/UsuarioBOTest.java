package me.Business.usuario;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.CredencialesInvalidasException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.ValidacionException;
import me.Business.ProteccionDatos;
import me.DAO.usuario.UsuarioDAO;
import me.Model.dto.usuario.CambioContraseniaDTO;
import me.Model.dto.usuario.CredencialesDTO;
import me.Model.dto.usuario.DatosPerfilDTO;
import me.Model.dto.usuario.RegistroUsuarioDTO;
import me.Model.entities.usuario.Rol;
import me.Model.entities.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioBOTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-07T01:00:00Z"), ZoneId.of("America/Lima"));
    private UsuarioDAO usuarios;
    private Usuario actor;
    private UsuarioBOImpl bo;

    @BeforeEach
    void preparar() throws Exception {
        usuarios = mock(UsuarioDAO.class);
        actor = new Usuario(1, "ana@test", ProteccionDatos.hash("ClaveSegura9"), "Ana",
                LocalDate.of(2000, 1, 1).atStartOfDay(), Rol.CLIENTE);
        actor.setUsuarioCreacion("ana@test");
        when(usuarios.leer(1)).thenReturn(actor);
        when(usuarios.obtenerPorEmail("ana@test")).thenReturn(actor);
        bo = new UsuarioBOImpl(usuarios, RELOJ);
    }

    @ParameterizedTest
    @CsvSource({"admin@admin.bookshop,ADMIN", "admin@tienda.test,CLIENTE", "persona@sub.admin.bookshop,CLIENTE"})
    void registroNormalizaCorreoYCalculaRolSinGuardarContraseniaOriginal(String email, Rol rol) throws Exception {
        doAnswer(inv -> {
            ((Usuario) inv.getArgument(0)).setId(10);
            return null;
        }).when(usuarios).insertar(any(Usuario.class));
        String clave = "  ClaveNueva9  ";
        var respuesta = bo.registrar(new RegistroUsuarioDTO("  Nombre Completo  ", " " + email.toUpperCase() + " ",
                LocalDate.of(2000, 1, 1), clave, clave, null));
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).insertar(guardado.capture());
        assertEquals(email, guardado.getValue().getEmail());
        assertEquals("Nombre Completo", guardado.getValue().getNombre());
        assertEquals(rol, guardado.getValue().getRol());
        assertNotEquals(clave, guardado.getValue().getContrasenia());
        assertTrue(ProteccionDatos.verificar(clave, guardado.getValue().getContrasenia()));
        assertFalse(ProteccionDatos.verificar(clave.strip(), guardado.getValue().getContrasenia()));
        assertEquals(10, respuesta.id());
        assertEquals(rol, respuesta.rol());
        assertFalse(respuesta.requiereReactivacion());
    }

    @Test
    void nacimientoUsaLaFechaDeLimaYLaValidacionNoInserta() throws Exception {
        assertThrows(ValidacionException.class, () -> bo.registrar(new RegistroUsuarioDTO("Ana", "nueva@test",
                LocalDate.of(2026, 10, 7), "ClaveNueva9", "ClaveNueva9", null)));
        // Es 6 de octubre en Lima aunque el instante UTC ya es del dia 7.
        verify(usuarios, never()).insertar(any());
        UsuarioBOImpl zonaUTC = new UsuarioBOImpl(usuarios, RELOJ.withZone(ZoneId.of("UTC")));
        assertThrows(ValidacionException.class, () -> zonaUTC.registrar(new RegistroUsuarioDTO("Ana", "nueva@test",
                LocalDate.of(2026, 10, 7), "ClaveNueva9", "ClaveNueva9", null)));
        assertThrows(ValidacionException.class, () -> bo.registrar(new RegistroUsuarioDTO("Ana", "nueva@test",
                LocalDate.of(2000, 1, 1), "ClaveNueva9", "clavenueva9", null)));
        verify(usuarios, never()).insertar(any());
    }

    @Test
    void correoDuplicadoEnLecturaOCarreraProduceConflicto() throws Exception {
        var datos = new RegistroUsuarioDTO("Ana", "ana@test", LocalDate.of(2000, 1, 1),
                "ClaveNueva9", "ClaveNueva9", null);
        assertThrows(ConflictoException.class, () -> bo.registrar(datos));
        verify(usuarios, never()).insertar(any());
        when(usuarios.obtenerPorEmail("ana@test")).thenReturn(null);
        doThrow(new SQLException("duplicado", "23505")).when(usuarios).insertar(any());
        assertThrows(ConflictoException.class, () -> bo.registrar(datos));
    }

    @Test
    void loginInactivoExigeClaveCorrectaAntesDeOfrecerReactivacion() throws Exception {
        actor.setActivo(false);
        assertThrows(CredencialesInvalidasException.class,
                () -> bo.iniciarSesion(new CredencialesDTO("ANA@test", "Incorrecta")));
        var acceso = bo.iniciarSesion(new CredencialesDTO(" ANA@TEST ", "ClaveSegura9"));
        assertEquals(1, acceso.id());
        assertTrue(acceso.requiereReactivacion());
        verify(usuarios, never()).cambiarActivo(anyInt(), anyBoolean(), anyString());
        when(usuarios.cambiarActivo(1, true, "ana@test")).thenReturn(true);
        assertFalse(bo.reactivar(new CredencialesDTO("ana@test", "ClaveSegura9")).requiereReactivacion());
        verify(usuarios).cambiarActivo(1, true, "ana@test");
        verify(usuarios, never()).insertar(any());
    }

    @Test
    void cambiarClaveRechazaActualIncorrectaYHashNuevoConConfirmacionExacta() throws Exception {
        assertThrows(CredencialesInvalidasException.class, () -> bo.cambiarContrasenia(1,
                new CambioContraseniaDTO("incorrecta", "ClaveNueva9", "ClaveNueva9")));
        verify(usuarios, never()).cambiarContrasenia(anyInt(), anyString(), anyString());
        when(usuarios.cambiarContrasenia(eq(1), anyString(), eq("ana@test"))).thenReturn(true);
        bo.cambiarContrasenia(1, new CambioContraseniaDTO("ClaveSegura9", "ClaveNueva9", "ClaveNueva9"));
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(usuarios).cambiarContrasenia(eq(1), hash.capture(), eq("ana@test"));
        assertTrue(ProteccionDatos.verificar("ClaveNueva9", hash.getValue()));
        assertFalse(ProteccionDatos.verificar("ClaveSegura9", hash.getValue()));
    }

    @Test
    void desactivarVerificaClaveYLaCuentaInactivaBloqueaOperacionesPersonales() throws Exception {
        assertThrows(CredencialesInvalidasException.class, () -> bo.desactivar(1, "incorrecta"));
        verify(usuarios, never()).cambiarActivo(anyInt(), anyBoolean(), anyString());
        when(usuarios.cambiarActivo(1, false, "ana@test")).thenAnswer(inv -> {
            actor.setActivo(false);
            return true;
        });
        bo.desactivar(1, "ClaveSegura9");
        assertThrows(CuentaInactivaException.class, () -> bo.obtenerPerfil(1));
        verify(usuarios, never()).eliminar(anyInt(), anyString());
    }

    @Test
    void perfilYCierreDeSesionMantienenCamposDeSeguridadYAcumulanTiempo() throws Exception {
        String hashOriginal = actor.getContrasenia();
        bo.modificarPerfil(1, new DatosPerfilDTO("Nuevo Nombre", LocalDate.of(1999, 1, 1)));
        ArgumentCaptor<Usuario> editado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).modificar(editado.capture());
        assertEquals("ana@test", editado.getValue().getEmail());
        assertEquals(Rol.CLIENTE, editado.getValue().getRol());
        assertEquals(hashOriginal, editado.getValue().getContrasenia());
        bo.cerrarSesion(1, Duration.ofMinutes(5));
        verify(usuarios).actualizarTiempoTotal(1, Duration.ofMinutes(5), "ana@test");
        assertThrows(ValidacionException.class, () -> bo.cerrarSesion(1, Duration.ofSeconds(-1)));
    }

    @Test
    void erroresSqlSeTraducenAExcepcionDeNegocio() throws Exception {
        SQLException error = new SQLException("fallo simulado");
        when(usuarios.obtenerPerfil(1)).thenThrow(error);
        PersistenciaException excepcion = assertThrows(PersistenciaException.class, () -> bo.obtenerPerfil(1));
        assertSame(error, excepcion.getCause());
    }
}
