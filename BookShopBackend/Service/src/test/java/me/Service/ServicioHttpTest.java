package me.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.io.StringReader;
import java.time.LocalDateTime;
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
import me.Business.exceptions.CuentaInactivaException;
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
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.ResumenPedidoDTO;
import me.Model.entities.compra.EstadoPedido;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
    void operacionPrivadaSinSesionNoLlamaBusiness() {
        try (Response respuesta = http.target("carrito").request().get()) {
            assertEquals(401, respuesta.getStatus());
            verifyNoInteractions(servicios.getCarritoBO());
        }
    }

    @Test
    void catalogoPublicoUsaVisitanteYRespetaFiltro() throws Exception {
        when(servicios.getLibroBO().buscar(eq(0), any())).thenReturn(new ResultadoPaginadoDTO<>(List.of(), 0, 1, 12));
        try (Response respuesta = http.target("libros/busqueda").request().post(Entity.json(FiltroLibroDTO.porDefecto()))) {
            assertEquals(200, respuesta.getStatus());
            verify(servicios.getLibroBO()).buscar(eq(0), argThat(f -> f.pagina() == 1 && f.soloActivos()));
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
        when(servicios.getLibroBO().buscar(anyInt(), any())).thenThrow(new ValidacionException("pagina", "Página inválida."));
        try (Response respuesta = http.target("libros/busqueda").request().post(Entity.json(FiltroLibroDTO.porDefecto()))) {
            assertEquals(400, respuesta.getStatus());
            String json = respuesta.readEntity(String.class);
            assertTrue(json.contains("\"campo\":\"pagina\""));
            assertFalse(json.contains("stackTrace"));
        }
    }

    @Test
    void errorInternoNoExponeSqlNiCausa() throws Exception {
        when(servicios.getLibroBO().buscar(anyInt(), any())).thenThrow(new PersistenciaException("SELECT secreto", new IllegalStateException("clave privada")));
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
    void noExponeRequisitosExcluidos() {
        for (String ruta : List.of("solicitudes", "configuracion", "documentos/reportes/usuarios/datos", "pedidos/123", "libros/123/gestion", "recomendaciones/para-ti/disponibilidad")) {
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
    @NullAndEmptySource
    @ValueSource(strings = { "fasle", "1" })
    void listaAusenteOInvalidaNoModificaDeseados(String favorito) {
        when(request.getSession(false)).thenReturn(sesion);
        atributos.put("bookshop.actor", 7);
        var destino = http.target("cuenta/libros-guardados/3");
        if (favorito != null)
            destino = destino.queryParam("favorito", favorito);
        for (String metodo : List.of("PUT", "DELETE")) {
            try (Response respuesta = destino.request().method(metodo)) {
                assertEquals(400, respuesta.getStatus());
                verifyNoInteractions(servicios.getLibroGuardadoBO());
            }
        }
        var busqueda = http.target("cuenta/libros-guardados/busqueda");
        if (favorito != null)
            busqueda = busqueda.queryParam("favorito", favorito);
        try (Response respuesta = busqueda.request().post(Entity.json(FiltroLibroDTO.porDefecto()))) {
            assertEquals(400, respuesta.getStatus());
            verifyNoInteractions(servicios.getLibroGuardadoBO());
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
        verify(servicios.getLibroBO()).buscarIdiomas(7, null, 2, 3);
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
    void paginaDeCatalogoConservaImportesYTotalDelConjunto() throws Exception {
        var libro = new TarjetaLibroDTO(3, "Libro", null, "Autor", new BigDecimal("19.99"), new BigDecimal("16.99"), new BigDecimal("15.00"), true);
        when(servicios.getLibroBO().buscar(eq(0), any())).thenReturn(new ResultadoPaginadoDTO<>(List.of(libro), 25, 2, 12));
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
