package me.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import me.Business.documentos.ComprobanteBO;
import me.Business.documentos.ReporteBO;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import jakarta.json.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import me.Business.catalogo.LibroBO;
import me.Business.compra.CarritoBO;
import me.Business.compra.CuponBO;
import me.Business.compra.PedidoBO;
import me.Business.opinion.ResenaBO;
import me.Business.opinion.VotoUtilBO;
import me.Business.recomendacion.RecomendacionBO;
import me.Business.solicitud.SolicitudBO;
import me.Model.dto.catalogo.ConsultaRecomendacionesDTO;
import me.Model.dto.catalogo.RecomendacionesDTO;
import me.Model.dto.catalogo.OrdenLibro;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.CredencialesInvalidasException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.ValidacionException;
import me.Business.usuario.DireccionEnvioBO;
import me.Business.usuario.LibroGuardadoBO;
import me.Business.usuario.MetodoPagoBO;
import me.Business.usuario.SeguimientoBO;
import me.Business.usuario.UsuarioBO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.dto.compra.DatosCancelacionDTO;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.FiltroPedidoDTO;
import me.Model.dto.compra.OrdenPedido;
import me.Model.dto.compra.ResumenPedidoDTO;
import me.Model.dto.compra.TarjetaPedidoDTO;
import me.Model.dto.opinion.ConsultaResenasDTO;
import me.Model.dto.opinion.DatosResenaDTO;
import me.Model.dto.opinion.OrdenResena;
import me.Model.dto.opinion.ResenaDTO;
import me.Model.dto.solicitud.DatosDevolucionDTO;
import me.Model.dto.solicitud.DatosReporteResenaDTO;
import me.Model.dto.solicitud.DetalleDevolucionDTO;
import me.Model.dto.solicitud.DetalleReporteResenaDTO;
import me.Model.dto.solicitud.FiltroSolicitudDTO;
import me.Model.dto.solicitud.ResenaReportadaDTO;
import me.Model.dto.solicitud.ResumenSolicitudDTO;
import me.Model.dto.solicitud.TipoSolicitud;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.solicitud.EstadoSolicitud;
import me.Model.entities.solicitud.MotivoDevolucion;
import me.Model.entities.solicitud.MotivoReporteResena;
import me.Model.dto.usuario.AccesoUsuarioDTO;
import me.Model.dto.usuario.CredencialesDTO;
import me.Model.entities.usuario.Rol;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServicioHttpTest {

    private JerseyTest http;

    private Servicios servicios;

    private HttpServletRequest request;

    private HttpSession sesion;

    private Map<String, Object> atributos;

    @BeforeEach
    void iniciar() throws Exception {
        servicios = mock(Servicios.class);
        when(servicios.getUsuarioBO()).thenReturn(mock(UsuarioBO.class));
        when(servicios.getLibroBO()).thenReturn(mock(LibroBO.class));
        when(servicios.getCarritoBO()).thenReturn(mock(CarritoBO.class));
        when(servicios.getPedidoBO()).thenReturn(mock(PedidoBO.class));
        when(servicios.getCuponBO()).thenReturn(mock(CuponBO.class));
        when(servicios.getLibroGuardadoBO()).thenReturn(mock(LibroGuardadoBO.class));
        when(servicios.getDireccionEnvioBO()).thenReturn(mock(DireccionEnvioBO.class));
        when(servicios.getMetodoPagoBO()).thenReturn(mock(MetodoPagoBO.class));
        when(servicios.getSeguimientoBO()).thenReturn(mock(SeguimientoBO.class));
        when(servicios.getComprobanteBO()).thenReturn(mock(ComprobanteBO.class));
        when(servicios.getReporteBO()).thenReturn(mock(ReporteBO.class));
        when(servicios.getRecomendacionBO()).thenReturn(mock(RecomendacionBO.class));
        when(servicios.getResenaBO()).thenReturn(mock(ResenaBO.class));
        when(servicios.getVotoUtilBO()).thenReturn(mock(VotoUtilBO.class));
        when(servicios.getSolicitudBO()).thenReturn(mock(SolicitudBO.class));
        request = mock(HttpServletRequest.class);
        sesion = mock(HttpSession.class);
        atributos = new HashMap<>();
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(sesion);
        when(sesion.getAttribute(anyString())).thenAnswer(inv -> atributos.get(inv.getArgument(0)));
        doAnswer(inv -> {
            atributos.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(sesion).setAttribute(anyString(), any());
        http = new JerseyTest() {

            @Override
            protected void configureClient(ClientConfig config) {
                // Permite probar PUT sin entidad; el contrato del servidor no exige cuerpo en esas rutas.
                config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
            }

            @Override
            protected Application configure() {
                ResourceConfig config = new ResourceConfig();
                new AplicacionRest().getClasses().forEach(config::register);
                config.register(new AbstractBinder() {

                    @Override
                    protected void configure() {
                        bind(servicios).to(Servicios.class);
                        bind(request).to(HttpServletRequest.class);
                    }
                });
                return config;
            }

            @Override
            protected TestContainerFactory getTestContainerFactory() {
                return new InMemoryTestContainerFactory();
            }
        };
        http.setUp();
    }

    @AfterEach
    void cerrar() throws Exception {
        if (http != null)
            http.tearDown();
    }

    @Test
    void popularesAbreUnConjuntoYConsultaConservaElIdentificador() throws Exception {
        var pagina = new ResultadoPaginadoDTO<TarjetaLibroDTO>(List.of(), 0, 1, 12);
        var consulta = new ConsultaRecomendacionesDTO(OrdenLibro.RECOMENDADOS_PRIMERO, 1, 12);
        when(servicios.getRecomendacionBO().abrirPopulares(consulta)).thenReturn(new RecomendacionesDTO("conjunto", pagina));
        when(servicios.getRecomendacionBO().consultar(eq(0), eq("conjunto"), any())).thenReturn(pagina);
        try (Response respuesta = http.target("recomendaciones/populares").request().post(Entity.json("{\"orden\":\"RECOMENDADOS_PRIMERO\",\"pagina\":1,\"tamanioPagina\":12}"))) {
            assertEquals(200, respuesta.getStatus());
            var json = Json.createReader(new StringReader(respuesta.readEntity(String.class))).readObject();
            assertEquals("conjunto", json.getString("idConsulta"));
            assertTrue(json.containsKey("resultados"));
        }
        try (Response respuesta = http.target("recomendaciones/consultas/conjunto").request().post(Entity.json("{\"orden\":\"PRECIO_ASCENDENTE\",\"pagina\":2,\"tamanioPagina\":12}"))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getRecomendacionBO()).consultar(0, "conjunto", new ConsultaRecomendacionesDTO(OrdenLibro.PRECIO_ASCENDENTE, 2, 12));
        }
        try (Response respuesta = http.target("recomendaciones/consultas/conjunto").request().delete()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getRecomendacionBO()).descartar(0, "conjunto");
        }
    }

    @Test
    void paraTiRequiereSesionYConsultaElHistorialDelUsuarioActual() throws Exception {
        try (Response respuesta = http.target("recomendaciones/para-ti").request().post(Entity.json("{}"))) {
            assertEquals(401, respuesta.getStatus());
            verifyNoInteractions(servicios.getRecomendacionBO());
        }
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        when(servicios.getRecomendacionBO().puedeMostrarParaTi(7)).thenReturn(true);
        try (Response respuesta = http.target("recomendaciones/para-ti/disponible").request().get()) {
            assertEquals(200, respuesta.getStatus());
            assertEquals("true", respuesta.readEntity(String.class));
        }
        var consulta = new ConsultaRecomendacionesDTO(OrdenLibro.RECOMENDADOS_PRIMERO, 1, 12);
        when(servicios.getRecomendacionBO().abrirParaTi(7, consulta)).thenReturn(new RecomendacionesDTO("propio", new ResultadoPaginadoDTO<>(List.of(), 0, 1, 12)));
        try (Response respuesta = http.target("recomendaciones/para-ti").request().post(Entity.json("{\"orden\":\"RECOMENDADOS_PRIMERO\",\"pagina\":1,\"tamanioPagina\":12}"))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getRecomendacionBO()).abrirParaTi(7, consulta);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "GET, resenas/libro/3/publicacion/permitida",
        "POST, resenas/libro/3",
        "PUT, resenas/4",
        "DELETE, resenas/4",
        "POST, resenas/propias/busqueda",
        "PUT, resenas/4/voto",
        "DELETE, resenas/4/voto",
        "GET, solicitudes/reportes/resena/4/permitido",
        "POST, solicitudes/reportes/resena/4"
    })
    void accionesRF10RequierenSesion(String metodo, String ruta) {
        try (Response respuesta = http.target(ruta).request().method(metodo)) {
            assertEquals(401, respuesta.getStatus());
            verifyNoInteractions(servicios.getResenaBO(), servicios.getVotoUtilBO(), servicios.getSolicitudBO());
        }
    }

    @Test
    void publicarYEditarResenaUsanLaIdentidadDeSesion() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        var datos = new DatosResenaDTO(4, "Comentario");
        when(servicios.getResenaBO().publicar(7, 3, datos)).thenReturn(42);

        try (Response respuesta = http.target("resenas/libro/3/publicacion/permitida").request().get()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getResenaBO()).verificarPuedePublicar(7, 3);
        }
        try (Response respuesta = http.target("resenas/libro/3").request().post(Entity.json(datos))) {
            assertEquals(200, respuesta.getStatus());
            assertEquals("42", respuesta.readEntity(String.class));
        }
        try (Response respuesta = http.target("resenas/42").request().put(Entity.json(datos))) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getResenaBO()).modificar(7, 42, datos);
        }
        try (Response respuesta = http.target("resenas/42").request().delete()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getResenaBO()).eliminarPropia(7, 42);
        }
        var consulta = new ConsultaResenasDTO(OrdenResena.VOTOS_UTILES_DESCENDENTE, 2, 10);
        when(servicios.getResenaBO().listarPropias(7, consulta))
            .thenReturn(new ResultadoPaginadoDTO<ResenaDTO>(List.of(), 12, 2, 10));
        try (Response respuesta = http.target("resenas/propias/busqueda").request().post(Entity.json(consulta))) {
            assertEquals(200, respuesta.getStatus());
            var json = Json.createReader(new StringReader(respuesta.readEntity(String.class))).readObject();
            assertEquals(12, json.getInt("totalResultados"));
            assertEquals(2, json.getInt("pagina"));
            verify(servicios.getResenaBO()).listarPropias(7, consulta);
        }
    }

    @Test
    void votoUtilDevuelveSiCambioAlMarcarYRetirarSinCuerpo() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        when(servicios.getVotoUtilBO().cambiarVoto(7, 4, true)).thenReturn(true, false);
        when(servicios.getVotoUtilBO().cambiarVoto(7, 4, false)).thenReturn(true, false);
        for (String metodo : List.of("PUT", "DELETE")) {
            for (boolean cambio : List.of(true, false)) {
                try (Response respuesta = http.target("resenas/4/voto").request().method(metodo)) {
                    assertEquals(200, respuesta.getStatus());
                    assertEquals(Boolean.toString(cambio), respuesta.readEntity(String.class));
                }
            }
        }
        verify(servicios.getVotoUtilBO(), times(2)).cambiarVoto(7, 4, true);
        verify(servicios.getVotoUtilBO(), times(2)).cambiarVoto(7, 4, false);
    }

    @Test
    void reportarCompruebaAntesDelFormularioYEnviaMotivoYComentario() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        var datos = new DatosReporteResenaDTO(MotivoReporteResena.PUBLICIDAD_O_SPAM, "Enlaces ajenos al libro");
        var resumen = new ResumenSolicitudDTO(9, TipoSolicitud.REPORTE_RESENA,
            LocalDateTime.of(2026, 10, 7, 10, 0), EstadoSolicitud.PENDIENTE, null, "Publicidad o spam", 4);
        var original = new ResenaReportadaDTO(4, "Ana", "Libro", new DatosResenaDTO(2, "Contenido original"));
        var detalle = new DetalleReporteResenaDTO(resumen, datos, original, null, true, null);
        when(servicios.getSolicitudBO().enviarReporte(7, 4, datos)).thenReturn(detalle);

        try (Response respuesta = http.target("solicitudes/reportes/resena/4/permitido").request().get()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).comprobarReporte(7, 4);
        }
        try (Response respuesta = http.target("solicitudes/reportes/resena/4").request().post(Entity.json(datos))) {
            assertEquals(200, respuesta.getStatus());
            var json = Json.createReader(new StringReader(respuesta.readEntity(String.class))).readObject();
            assertEquals(9, json.getJsonObject("solicitud").getInt("id"));
            assertEquals("Contenido original", json.getJsonObject("resenaReportada")
                .getJsonObject("contenidoOriginal").getString("comentario"));
            verify(servicios.getSolicitudBO()).enviarReporte(7, 4, datos);
        }
        SolicitudBO solicitudes = servicios.getSolicitudBO();
        doThrow(new me.Business.exceptions.ConflictoException("Ya tienes un reporte pendiente de esta reseña."))
            .when(solicitudes).comprobarReporte(7, 4);
        try (Response respuesta = http.target("solicitudes/reportes/resena/4/permitido").request().get()) {
            assertEquals(409, respuesta.getStatus());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "POST, pedidos/busqueda",
        "PUT, pedidos/3/seguimiento",
        "POST, pedidos/3/cancelacion",
        "GET, solicitudes/devoluciones/pedido/3/permitido",
        "POST, solicitudes/devoluciones/pedido/3",
        "POST, solicitudes/propias/busqueda",
        "GET, solicitudes/devoluciones/9",
        "GET, solicitudes/reportes/9",
        "PUT, solicitudes/devoluciones/9",
        "PUT, solicitudes/reportes/9",
        "DELETE, solicitudes/9",
        "GET, solicitudes/9/pedido",
        "POST, solicitudes/moderacion/busqueda",
        "PUT, solicitudes/devoluciones/9/resolucion",
        "PUT, solicitudes/reportes/9/resolucion"
    })
    void accionesRF03RequierenSesion(String metodo, String ruta) {
        try (Response respuesta = http.target(ruta).request().method(metodo)) {
            assertEquals(401, respuesta.getStatus());
            verifyNoInteractions(servicios.getPedidoBO(), servicios.getSolicitudBO());
        }
    }

    @Test
    void historialSeguimientoYCancelacionUsanSesionYConservanElCuerpoDelFormulario() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        var fecha = LocalDateTime.of(2026, 10, 7, 10, 0);
        var pedido = new ResumenPedidoDTO(3, fecha, EstadoPedido.EN_ALMACEN, 2, new BigDecimal("35.68"));
        var filtro = new FiltroPedidoDTO(List.of(EstadoPedido.EN_ALMACEN, EstadoPedido.CANCELADO),
            OrdenPedido.TOTAL_DESCENDENTE, 2, 10);
        var tarjeta = new TarjetaPedidoDTO(pedido, List.of("portada"));
        when(servicios.getPedidoBO().listar(7, filtro)).thenReturn(new ResultadoPaginadoDTO<>(List.of(tarjeta), 12, 2, 10));
        var detalle = new DetallePedidoDTO(pedido, null, null, null, "TRANSACCIÓN", "SEGUIMIENTO",
            fecha.plusSeconds(90), null, null, true, false);
        when(servicios.getPedidoBO().actualizarSeguimiento(7, 3)).thenReturn(detalle);
        when(servicios.getPedidoBO().cancelar(7, 3, "Ya no lo necesito")).thenReturn(detalle);

        try (Response respuesta = http.target("pedidos/busqueda").request().post(Entity.json(filtro))) {
            assertEquals(200, respuesta.getStatus());
            var json = Json.createReader(new StringReader(respuesta.readEntity(String.class))).readObject();
            assertEquals(12, json.getInt("totalResultados"));
            assertEquals("portada", json.getJsonArray("items").getJsonObject(0).getJsonArray("portadas").getString(0));
            verify(servicios.getPedidoBO()).listar(7, filtro);
        }
        try (Response respuesta = http.target("pedidos/3/seguimiento").request().method("PUT")) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getPedidoBO()).actualizarSeguimiento(7, 3);
        }
        try (Response respuesta = http.target("pedidos/3/cancelacion").request()
            .post(Entity.json(new DatosCancelacionDTO("Ya no lo necesito")))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getPedidoBO()).cancelar(7, 3, "Ya no lo necesito");
        }
    }

    @Test
    void devolucionSeCompruebaAntesDelFormularioYLasSolicitudesSeConsultanPorSuTipo() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        var datos = new DatosDevolucionDTO(MotivoDevolucion.LIBRO_DANADO, "Portada dañada");
        var resumen = new ResumenSolicitudDTO(9, TipoSolicitud.DEVOLUCION,
            LocalDateTime.of(2026, 10, 7, 10, 0), EstadoSolicitud.PENDIENTE, null, "Libro dañado", 3);
        var pedido = new ResumenPedidoDTO(3, resumen.fechaSolicitud(), EstadoPedido.ENTREGADO, 2, BigDecimal.TEN);
        var detalle = new DetalleDevolucionDTO(resumen, datos, pedido, null);
        when(servicios.getSolicitudBO().enviarDevolucion(7, 3, datos)).thenReturn(detalle);
        when(servicios.getSolicitudBO().consultarDevolucion(7, 9)).thenReturn(detalle);
        when(servicios.getSolicitudBO().editarDevolucion(7, 9, datos)).thenReturn(detalle);

        try (Response respuesta = http.target("solicitudes/devoluciones/pedido/3/permitido").request().get()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).comprobarDevolucion(7, 3);
        }
        try (Response respuesta = http.target("solicitudes/devoluciones/pedido/3").request().post(Entity.json(datos))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).enviarDevolucion(7, 3, datos);
        }
        try (Response respuesta = http.target("solicitudes/devoluciones/9").request().get()) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).consultarDevolucion(7, 9);
        }
        try (Response respuesta = http.target("solicitudes/devoluciones/9").request().put(Entity.json(datos))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).editarDevolucion(7, 9, datos);
        }
        try (Response respuesta = http.target("solicitudes/reportes/10").request().get()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).consultarReporte(7, 10);
        }
        var reporte = new DatosReporteResenaDTO(MotivoReporteResena.OTRO, "Aclaración");
        try (Response respuesta = http.target("solicitudes/reportes/10").request().put(Entity.json(reporte))) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).editarReporte(7, 10, reporte);
        }
        try (Response respuesta = http.target("solicitudes/9").request().delete()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).retirar(7, 9);
        }
        try (Response respuesta = http.target("solicitudes/9/pedido").request().get()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).consultarPedidoReferencia(7, 9);
        }
    }

    @Test
    void listasYModeracionRecibenFiltrosYResolucionesSinOtrasConsultasDeRol() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        var filtro = new FiltroSolicitudDTO(TipoSolicitud.REPORTE_RESENA,
            List.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.CERRADA), 2, 10);
        when(servicios.getSolicitudBO().listarPropias(7, filtro)).thenReturn(new ResultadoPaginadoDTO<>(List.of(), 12, 2, 10));
        when(servicios.getSolicitudBO().listarModeracion(filtro)).thenReturn(new ResultadoPaginadoDTO<>(List.of(), 12, 2, 10));
        try (Response respuesta = http.target("solicitudes/propias/busqueda").request().post(Entity.json(filtro))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).listarPropias(7, filtro);
        }
        try (Response respuesta = http.target("solicitudes/moderacion/busqueda").request().post(Entity.json(filtro))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getSolicitudBO()).listarModeracion(filtro);
        }
        for (EstadoSolicitud resultado : List.of(EstadoSolicitud.ACEPTADA, EstadoSolicitud.RECHAZADA)) {
            try (Response respuesta = http.target("solicitudes/devoluciones/9/resolucion").queryParam("resultado", resultado).request().method("PUT")) {
                assertEquals(204, respuesta.getStatus());
                verify(servicios.getSolicitudBO()).resolverDevolucion(7, 9, resultado);
            }
        }
        for (EstadoSolicitud resultado : List.of(EstadoSolicitud.ACEPTADA, EstadoSolicitud.RECHAZADA, EstadoSolicitud.CERRADA)) {
            try (Response respuesta = http.target("solicitudes/reportes/10/resolucion").queryParam("resultado", resultado).request().method("PUT")) {
                assertEquals(204, respuesta.getStatus());
                verify(servicios.getSolicitudBO()).resolverReporte(7, 10, resultado);
            }
        }
        verifyNoInteractions(servicios.getUsuarioBO());
    }

    @Test
    void comprobanteRF17DevuelvePdfDelPedidoDeLaSesion() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        when(servicios.getComprobanteBO().generarPDF(7, 40)).thenReturn("%PDF-1.5".getBytes());
        try (Response respuesta = http.target("documentos/comprobantes/40").request("application/pdf").get()) {
            assertEquals(200, respuesta.getStatus());
            assertEquals("application/pdf", respuesta.getMediaType().toString());
            assertTrue(respuesta.getHeaderString("Content-Disposition").contains("comprobante-pedido-40.pdf"));
            assertArrayEquals("%PDF-1.5".getBytes(), respuesta.readEntity(byte[].class));
        }
    }

    @Test
    void comprobanteSinSesionNoLlamaBusiness() {
        try (Response respuesta = http.target("documentos/comprobantes/40").request("application/pdf").get()) {
            assertEquals(401, respuesta.getStatus());
            verifyNoInteractions(servicios.getComprobanteBO());
        }
    }

    @Test
    void comprobanteAjenoConservaElRechazoDeBusiness() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        when(servicios.getComprobanteBO().generarPDF(7, 40))
            .thenThrow(new me.Business.exceptions.AccesoDenegadoException("El pedido pertenece a otro usuario."));
        try (Response respuesta = http.target("documentos/comprobantes/40").request("application/pdf").get()) {
            assertEquals(403, respuesta.getStatus());
        }
    }

    @Test
    void reportesRF18YRF19ConviertenParametros() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 1);
        when(servicios.getReporteBO().generarLibrosPDF(any())).thenReturn(new byte[] {1});
        when(servicios.getReporteBO().generarUsuariosPDF(any(), any())).thenReturn(new byte[] {2});
        try (Response respuesta = http.target("documentos/ventas-libros").queryParam("inicio", "2026-10-01")
                .queryParam("fin", "2026-10-31").queryParam("cantidad", 5).request("application/pdf").get()) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getReporteBO()).generarLibrosPDF(new ConsultaReporteLibrosDTO(
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 5));
        }
        try (Response respuesta = http.target("documentos/usuarios").queryParam("mes", "2026-10")
                .request("application/pdf").get()) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getReporteBO()).generarUsuariosPDF(YearMonth.of(2026, 10), null);
        }
        try (Response respuesta = http.target("documentos/usuarios").queryParam("mes", "octubre")
                .request("application/pdf").get()) {
            assertEquals(400, respuesta.getStatus());
        }
    }

    @Test
    void reporteSinSesionNoLlamaBusiness() {
        try (Response respuesta = http.target("documentos/usuarios").queryParam("mes", "2026-10")
                .request("application/pdf").get()) {
            assertEquals(401, respuesta.getStatus());
            verifyNoInteractions(servicios.getReporteBO());
        }
    }

    @Test
    void operacionPrivadaSinSesionNoLlamaBusiness() {
        try (Response respuesta = http.target("carrito").request().get()) {
            assertEquals(401, respuesta.getStatus());
            verifyNoInteractions(servicios.getCarritoBO());
        }
    }

    @Test
    void catalogoPublicoUsaVisitanteYRespetaFiltro() throws Exception {
        when(servicios.getLibroBO().buscar(any())).thenReturn(new ResultadoPaginadoDTO<>(List.of(), 0, 1, 12));
        try (Response respuesta = http.target("libros/busqueda").request().post(Entity.json(FiltroLibroDTO.porDefecto()))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getLibroBO()).buscar(argThat(f -> f.pagina() == 1 && f.soloActivos()));
        }
    }

    @Test
    void loginCreaSesionYRenuevaIdentificador() throws Exception {
        when(servicios.getUsuarioBO().iniciarSesion(any())).thenReturn(new AccesoUsuarioDTO(7, "Ana", Rol.CLIENTE, false));
        try (Response respuesta = http.target("cuenta/sesion").request().post(Entity.json(new CredencialesDTO("ana@correo.pe", "clave123")))) {
            assertEquals(200, respuesta.getStatus());
            assertEquals(7, atributos.get("bookshop.actor"));
        }
    }

    @Test
    void cuentaInactivaNoObtieneSesionPrivada() throws Exception {
        when(servicios.getUsuarioBO().iniciarSesion(any())).thenReturn(new AccesoUsuarioDTO(7, "Ana", Rol.CLIENTE, true));
        try (Response respuesta = http.target("cuenta/sesion").request().post(Entity.json(new CredencialesDTO("ana@correo.pe", "clave123")))) {
            assertEquals(200, respuesta.getStatus());
            verify(request, never()).getSession(true);
            assertTrue(atributos.isEmpty());
        }
    }

    @Test
    void actorDeConsultaNoSustituyeLaSesion() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target("pedidos/confirmacion").queryParam("idActor", 99).request().post(Entity.json(""))) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getPedidoBO()).pagar(7);
            verify(servicios.getPedidoBO(), never()).pagar(99);
        }
    }

    @Test
    void errorDeValidacionExponeCampoSinTraza() throws Exception {
        when(servicios.getLibroBO().buscar(any())).thenThrow(new ValidacionException("pagina", "Página inválida."));
        try (Response respuesta = http.target("libros/busqueda").request().post(Entity.json(FiltroLibroDTO.porDefecto()))) {
            assertEquals(400, respuesta.getStatus());
            String json = respuesta.readEntity(String.class);
            assertTrue(json.contains("\"campo\":\"pagina\""));
            assertFalse(json.contains("stackTrace"));
        }
    }

    @Test
    void errorInternoNoExponeSqlNiCausa() throws Exception {
        when(servicios.getLibroBO().buscar(any())).thenThrow(new PersistenciaException("SELECT secreto", new IllegalStateException("clave privada")));
        try (Response respuesta = http.target("libros/busqueda").request().post(Entity.json(FiltroLibroDTO.porDefecto()))) {
            assertEquals(500, respuesta.getStatus());
            String json = respuesta.readEntity(String.class);
            assertFalse(json.contains("SELECT"));
            assertFalse(json.contains("clave privada"));
        }
    }

    @Test
    void jsonInvalidoSeRechazaSinInvocarNegocio() {
        try (Response respuesta = http.target("cuenta/sesion").request().post(Entity.json("{mal"))) {
            assertEquals(400, respuesta.getStatus());
            verifyNoInteractions(servicios.getUsuarioBO());
        }
    }

    @Test
    void registroRecibeFechaIso() throws Exception {
        when(servicios.getUsuarioBO().registrar(any())).thenReturn(new AccesoUsuarioDTO(7, "Ana", Rol.CLIENTE, false));
        String json = "{\"nombre\":\"Ana\",\"email\":\"ana@correo.pe\",\"fechaNacimiento\":\"2000-02-29\"," + "\"contrasenia\":\"clave123\",\"confirmacionContrasenia\":\"clave123\",\"imagen\":null}";
        try (Response respuesta = http.target("cuenta/registro").request().post(Entity.json(json))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getUsuarioBO()).registrar(argThat(datos -> datos.fechaNacimiento().equals(java.time.LocalDate.of(2000, 2, 29))));
        }
    }

    @Test
    void cerrarSesionRegistraTiempoAntesDeInvalidar() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        atributos.put("bookshop.inicio", java.time.Instant.now().minusSeconds(10));
        try (Response respuesta = http.target("cuenta/sesion").request().delete()) {
            assertEquals(204, respuesta.getStatus());
            var orden = inOrder(servicios.getUsuarioBO(), sesion);
            orden.verify(servicios.getUsuarioBO()).cerrarSesion(eq(7), argThat(tiempo -> tiempo.toSeconds() >= 10));
            orden.verify(sesion).invalidate();
        }
    }

    @Test
    void imagenLlegaComoUrl() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target("cuenta/imagen").queryParam("url", "https://example.test/imagen.png").request().put(Entity.json("null"))) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getUsuarioBO()).cambiarImagen(eq(7), argThat(imagen -> "https://example.test/imagen.png".equals(imagen)));
        }
    }

    @Test
    void operacionPrivadaUsaLaIdentidadDeSesionSinOtraConsultaDeCuenta() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target("carrito").request().get()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getCarritoBO()).consultar(7);
            verifyNoInteractions(servicios.getUsuarioBO());
        }
    }

    @Test
    void consultarConfirmacionExigeSesionYUsaSuPropietario() throws Exception {
        try (Response respuesta = http.target("pedidos/123").request().get()) {
            assertEquals(401, respuesta.getStatus());
        }
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target("pedidos/123").request().get()) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getPedidoBO()).consultar(7, 123);
        }
    }

    @Test
    void noExponeRequisitosExcluidos() {
        for (String ruta : List.of("configuracion", "documentos/reportes/usuarios/datos", "libros/123/gestion", "recomendaciones/para-ti/disponibilidad")) {
            try (Response respuesta = http.target(ruta).request().get()) {
                assertEquals(404, respuesta.getStatus(), ruta);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT" })
    void cantidadOmitidaNoModificaElCarrito(String metodo) {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target("carrito/libros/3").request().method(metodo)) {
            assertEquals(400, respuesta.getStatus());
            verifyNoInteractions(servicios.getCarritoBO());
        }
    }

    @Test
    void ceroExplicitoConservaLaOperacionDeRetirarLinea() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target("carrito/libros/3").queryParam("cantidad", 0).request().method("PUT")) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getCarritoBO()).cambiarCantidad(7, 3, 0);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void listaExplicitaSeEntregaSinCambiosABusiness(boolean favorito) throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target("cuenta/libros-guardados/3").queryParam("favorito", favorito).request().method("PUT")) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getLibroGuardadoBO()).guardar(7, 3, favorito);
        }
        try (Response respuesta = http.target("cuenta/libros-guardados/3").queryParam("favorito", favorito).request().delete()) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getLibroGuardadoBO()).retirar(7, 3, favorito);
        }
        try (Response respuesta = http.target("cuenta/libros-guardados/busqueda").queryParam("favorito", favorito)
            .request().post(Entity.json(FiltroLibroDTO.porDefecto()))) {
            assertEquals(204, respuesta.getStatus());
            verify(servicios.getLibroGuardadoBO()).listar(eq(7), eq(favorito), any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "pedidos/preparacion", "pedidos/confirmacion" })
    void compraAceptaPostSinCuerpoNiTipoDeContenido(String ruta) {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target(ruta).request().method("POST")) {
            assertEquals(204, respuesta.getStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "cuenta/direcciones/3/predeterminada", "cuenta/metodos/3/predeterminado", "cuenta/seguimientos/AUTOR/3" })
    void seleccionPropiaAceptaPutSinCuerpoNiTipoDeContenido(String ruta) {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        try (Response respuesta = http.target(ruta).request().method("PUT")) {
            assertEquals(204, respuesta.getStatus());
        }
    }

    @Test
    void tamanioPaginaSeAplicaIgualEnIdiomasYCupones() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        for (String ruta : List.of("libros/idiomas", "cuenta/cupones")) {
            try (Response respuesta = http.target(ruta).queryParam("pagina", 2).queryParam("tamanioPagina", 3).request().get()) {
                assertEquals(204, respuesta.getStatus());
            }
        }
        verify(servicios.getLibroBO()).buscarIdiomas(null, 2, 3);
        verify(servicios.getCuponBO()).listar(7, 2, 3);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void falloAlRegistrarTiempoNoImpideCerrarSesion(boolean cuentaInactiva) throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        atributos.put("bookshop.inicio", java.time.Instant.now().minusSeconds(10));
        UsuarioBO usuarios = servicios.getUsuarioBO();
        if (cuentaInactiva) {
            doThrow(new CuentaInactivaException("Cuenta desactivada.")).when(usuarios).cerrarSesion(eq(7), any());
        } else {
            doThrow(new PersistenciaException("No se pudo guardar el tiempo.", new IllegalStateException())).when(usuarios).cerrarSesion(eq(7), any());
        }
        try (Response respuesta = http.target("cuenta/sesion").request().delete()) {
            assertEquals(cuentaInactiva ? 403 : 500, respuesta.getStatus());
            verify(sesion).invalidate();
        }
    }

    @Test
    void desactivarValidaLaClaveUnaVezYConservaElTiempoDeSesion() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        atributos.put("bookshop.inicio", java.time.Instant.now().minusSeconds(10));
        UsuarioBO usuarios = servicios.getUsuarioBO();
        try (Response respuesta = http.target("cuenta/desactivacion").request()
            .post(Entity.json("{\"contraseniaActual\":\"clave123\"}"))) {
            assertEquals(204, respuesta.getStatus());
            var orden = inOrder(usuarios, sesion);
            orden.verify(usuarios).desactivar(7, "clave123");
            orden.verify(usuarios).cerrarSesion(eq(7), argThat(tiempo -> tiempo.toSeconds() >= 10));
            orden.verify(sesion).invalidate();
            verify(usuarios, never()).obtenerPerfil(anyInt());
            verify(usuarios, never()).iniciarSesion(any());
        }
    }

    @Test
    void claveIncorrectaAlDesactivarConservaLaSesion() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        UsuarioBO usuarios = servicios.getUsuarioBO();
        doThrow(new CredencialesInvalidasException("La contraseña no es correcta."))
            .when(usuarios).desactivar(7, "incorrecta");
        try (Response respuesta = http.target("cuenta/desactivacion").request()
            .post(Entity.json("{\"contraseniaActual\":\"incorrecta\"}"))) {
            assertEquals(401, respuesta.getStatus());
            verify(usuarios, never()).cerrarSesion(anyInt(), any());
            verify(sesion, never()).invalidate();
        }
    }

    @Test
    void cuentaDesactivadaCierraSesionAunqueFalleGuardarSuTiempo() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        atributos.put("bookshop.inicio", java.time.Instant.now().minusSeconds(10));
        UsuarioBO usuarios = servicios.getUsuarioBO();
        doThrow(new PersistenciaException("No se pudo guardar el tiempo.", new IllegalStateException()))
            .when(usuarios).cerrarSesion(eq(7), any());
        try (Response respuesta = http.target("cuenta/desactivacion").request()
            .post(Entity.json("{\"contraseniaActual\":\"clave123\"}"))) {
            assertEquals(500, respuesta.getStatus());
            verify(usuarios).desactivar(7, "clave123");
            verify(sesion).invalidate();
        }
    }

    @Test
    void paginaDeCatalogoConservaImportesYTotalDelConjunto() throws Exception {
        var libro = new TarjetaLibroDTO(3, "Libro", null, "Autor", new BigDecimal("19.99"), new BigDecimal("16.99"), new BigDecimal("15.00"), true);
        when(servicios.getLibroBO().buscar(any())).thenReturn(new ResultadoPaginadoDTO<>(List.of(libro), 25, 2, 12));
        try (Response respuesta = http.target("libros/busqueda").request().post(Entity.json(FiltroLibroDTO.porDefecto()))) {
            assertEquals(200, respuesta.getStatus());
            try (var lector = Json.createReader(new StringReader(respuesta.readEntity(String.class)))) {
                var pagina = lector.readObject();
                assertEquals(25, pagina.getInt("totalResultados"));
                assertEquals(2, pagina.getInt("pagina"));
                var tarjeta = pagina.getJsonArray("items").getJsonObject(0);
                assertEquals(0, new BigDecimal("16.99").compareTo(tarjeta.getJsonNumber("precioConDescuento").bigDecimalValue()));
            }
        }
    }

    @Test
    void confirmacionDevuelveFechaEstadoEImporteDelPedido() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 4, 10, 0);
        var cabecera = new ResumenPedidoDTO(15, fecha, EstadoPedido.EN_ALMACEN, 2, new BigDecimal("35.68"));
        var detalle = new DetallePedidoDTO(cabecera, null, null, null, "TRANSACCION", "SEGUIMIENTO", fecha.plusSeconds(90), null, null, true, false);
        when(servicios.getPedidoBO().pagar(7)).thenReturn(detalle);
        try (Response respuesta = http.target("pedidos/confirmacion").request().method("POST")) {
            assertEquals(200, respuesta.getStatus());
            try (var lector = Json.createReader(new StringReader(respuesta.readEntity(String.class)))) {
                var pedido = lector.readObject().getJsonObject("pedido");
                assertEquals(fecha, LocalDateTime.parse(pedido.getString("fechaPago")));
                assertEquals("EN_ALMACEN", pedido.getString("estado"));
                assertEquals(0, new BigDecimal("35.68").compareTo(pedido.getJsonNumber("total").bigDecimalValue()));
            }
        }
    }
}
