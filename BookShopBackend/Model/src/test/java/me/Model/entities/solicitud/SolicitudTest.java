package me.Model.entities.solicitud;

import me.Model.entities.opinion.Resena;
import me.Model.entities.usuario.Usuario;
import me.Model.entities.compra.Pedido;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudTest {
    private final LocalDateTime fecha = LocalDateTime.of(2026, 10, 5, 12, 0);

    @Test
    void ambasSolicitudesCompartenEdicionResolucionYAuditoriaPorPolimorfismo() {
        Resena resena = new Resena();
        Solicitud[] solicitudes = {
                new SolicitudDevolucion(1, new Usuario(), new Pedido(), MotivoDevolucion.LIBRO_DANADO, "Comentario"),
                new SolicitudReporteResena(2, new Usuario(), resena,
                        MotivoReporteResena.CONTENIDO_OFENSIVO, "Comentario")
        };
        for (Solicitud solicitud : solicitudes) {
            assertTrue(solicitud.estaPendiente());
            solicitud.editar("Nuevo comentario", "cliente@bookshop", fecha);
            assertEquals("Nuevo comentario", solicitud.getComentario());
            assertEquals("cliente@bookshop", solicitud.getUsuarioActualizacion());
            solicitud.resolver(EstadoSolicitud.ACEPTADA, "uno@admin.bookshop", fecha.plusHours(1));
            assertTrue(solicitud.estaResuelta());
            assertFalse(solicitud.estaPendiente());
            assertEquals(fecha.plusHours(1), solicitud.getFechaResolucion());
            assertEquals("uno@admin.bookshop", solicitud.getUsuarioActualizacion());
            assertThrows(IllegalStateException.class,
                    () -> solicitud.editar("Otro", "cliente@bookshop", fecha));
            assertThrows(IllegalStateException.class, () -> solicitud.retirar("cliente@bookshop", fecha));
            assertThrows(IllegalStateException.class,
                    () -> solicitud.resolver(EstadoSolicitud.RECHAZADA, "uno@admin.bookshop", fecha));
        }
    }

    @Test
    void retirarUnaPendienteNoLaApruebaYBloqueaSuResolucion() {
        Solicitud solicitud = new SolicitudDevolucion();
        solicitud.retirar("cliente@bookshop", fecha);
        assertTrue(solicitud.isEliminado());
        assertEquals(EstadoSolicitud.PENDIENTE, solicitud.getEstado());
        assertFalse(solicitud.estaPendiente());
        assertFalse(solicitud.estaResuelta());
        assertNull(solicitud.getFechaResolucion());
        assertThrows(IllegalStateException.class,
                () -> solicitud.resolver(EstadoSolicitud.ACEPTADA, "uno@admin.bookshop", fecha));
    }

    @Test
    void reporteConservaElContenidoOriginalYConsultaElActualYDisponible() {
        Resena resena = new Resena();
        resena.setId(1);
        resena.setComentario("Original");
        resena.setPuntuacion(2);
        SolicitudReporteResena solicitud = new SolicitudReporteResena(
                1, new Usuario(), resena, MotivoReporteResena.CONTENIDO_OFENSIVO, "Revision");
        resena.setComentario("Editado");
        resena.setPuntuacion(4);
        assertEquals("Original", solicitud.getComentarioOriginal());
        assertEquals(2, solicitud.getPuntuacionOriginal());
        assertEquals("Editado", solicitud.getComentarioActual());
        assertTrue(solicitud.isResenaActiva());
        resena.setEliminado(true);
        assertFalse(solicitud.isResenaActiva());
        solicitud.resolver(EstadoSolicitud.CERRADA, "uno@admin.bookshop", fecha);
        assertEquals(EstadoSolicitud.CERRADA, solicitud.getEstado());
    }

    @Test
    void editarUnaSolicitudNoPermiteCambiarSuObjeto() {
        Pedido pedido = new Pedido();
        pedido.setId(1);
        SolicitudDevolucion solicitud = new SolicitudDevolucion(
                1, new Usuario(), pedido, MotivoDevolucion.OTRO, "");
        Pedido otro = new Pedido();
        otro.setId(2);
        assertThrows(IllegalStateException.class, () -> solicitud.setPedido(otro));
        assertSame(pedido, solicitud.getPedido());
    }

    @Test
    void editarReporteActualizaMotivoEnumeradoSinCambiarOriginalYRespetaEstado() {
        Resena resena = new Resena();
        resena.setComentario("Original");
        SolicitudReporteResena reporte = new SolicitudReporteResena(
                1, new Usuario(), resena, MotivoReporteResena.CONTENIDO_OFENSIVO, "Comentario");
        reporte.editar(MotivoReporteResena.PUBLICIDAD_O_SPAM, "Ampliacion", "cliente@bookshop", fecha);
        assertEquals(MotivoReporteResena.PUBLICIDAD_O_SPAM, reporte.getMotivo());
        assertEquals("Ampliacion", reporte.getComentario());
        assertEquals("Original", reporte.getComentarioOriginal());
        reporte.resolver(EstadoSolicitud.RECHAZADA, "uno@admin.bookshop", fecha.plusHours(1));
        assertThrows(IllegalStateException.class,
                () -> reporte.editar(MotivoReporteResena.OTRO, "Otro", "cliente@bookshop", fecha));
        assertEquals(MotivoReporteResena.PUBLICIDAD_O_SPAM, reporte.getMotivo());
        assertEquals("Ampliacion", reporte.getComentario());
    }

    @Test
    void editarDevolucionActualizaMotivoEnumeradoYRespetaEstado() {
        Pedido pedido = new Pedido();
        SolicitudDevolucion solicitud = new SolicitudDevolucion(
                1, new Usuario(), pedido, MotivoDevolucion.LIBRO_DANADO, "Comentario");
        solicitud.editar(MotivoDevolucion.PEDIDO_INCOMPLETO, "Falta un libro", "cliente@bookshop", fecha);
        assertEquals(MotivoDevolucion.PEDIDO_INCOMPLETO, solicitud.getMotivo());
        assertEquals("Falta un libro", solicitud.getComentario());
        assertEquals("cliente@bookshop", solicitud.getUsuarioActualizacion());
        assertSame(pedido, solicitud.getPedido());
        solicitud.resolver(EstadoSolicitud.RECHAZADA, "uno@admin.bookshop", fecha.plusHours(1));
        assertThrows(IllegalStateException.class,
                () -> solicitud.editar(MotivoDevolucion.OTRO, "Otro", "cliente@bookshop", fecha));
        assertEquals(MotivoDevolucion.PEDIDO_INCOMPLETO, solicitud.getMotivo());
        assertEquals("Falta un libro", solicitud.getComentario());
    }
}
