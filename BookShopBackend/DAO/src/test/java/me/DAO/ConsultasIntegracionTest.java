package me.DAO;

import me.DAO.venta.PedidoDAOImpl;

import me.DAO.venta.ItemPedidoDAOImpl;

import me.DAO.venta.CuponDAOImpl;

import me.DAO.usuario.UsuarioDAOImpl;

import me.DAO.usuario.SeguimientoDAOImpl;

import me.DAO.usuario.MetodoPagoDAOImpl;

import me.DAO.usuario.LibroGuardadoDAOImpl;

import me.DAO.usuario.DireccionEnvioDAOImpl;

import me.DAO.solicitud.SolicitudDAOImpl;

import me.DAO.catalogo.VotoUtilDAOImpl;

import me.DAO.catalogo.ResenaDAOImpl;

import me.DAO.catalogo.LibroDAOImpl;

import me.DAO.catalogo.DescuentoDAOImpl;

import me.DAO.catalogo.CategoriaDAOImpl;

import me.DAO.catalogo.AutorDAOImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.DBManager.DBManager;
import me.DAO.catalogo.*;
import me.DAO.usuario.*;
import me.DAO.venta.*;
import me.DAO.solicitud.*;
import me.DBManager.TransactionContext;
import me.Model.dto.catalogo.*;
import me.Model.dto.solicitud.FiltroSolicitudDTO;
import me.Model.dto.compra.FiltroPedidoDTO;
import me.Model.dto.compra.OrdenPedido;
import me.Model.dto.usuario.TipoSeguimiento;
import me.Model.entities.catalogo.*;
import me.Model.entities.opinion.*;
import me.Model.dto.opinion.*;
import me.Model.entities.usuario.*;
import me.Model.entities.compra.*;
import me.Model.entities.solicitud.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultasIntegracionTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 6, 12, 0);

    private Connection conexion;

    private MockedStatic<DBManager> manager;

    private String url;

    @BeforeEach
    void prepararBaseAislada() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        conexion = DriverManager.getConnection(url);
        // H2 prueba consultas y mapeos. CHECK/FK y sintaxis exclusiva se verifican aparte para MySQL.
        String ddl = Files.readString(Path.of("src/main/resources/DDL.sql")).replaceAll(",\\r?\\n {8,}", ", ");
        Matcher tablas = Pattern.compile("CREATE TABLE (\\w+) \\((.*?)\\) ENGINE", Pattern.DOTALL).matcher(ddl);
        while (tablas.find()) {
            List<String> columnas = new java.util.ArrayList<>();
            Matcher campos = Pattern.compile("(?m)^    ([a-z_]+) (INT|BIGINT|TINYINT|VARCHAR|TEXT|MEDIUMBLOB|" + "DATETIME|DATE|BOOLEAN|DECIMAL|ENUM)[^\\n]*").matcher(tablas.group(2));
            while (campos.find()) {
                String campo = campos.group().trim().replaceAll(",?$", "").replaceAll(" COMMENT '[^']*'", "");
                if (campo.contains("GENERATED"))
                    continue;
                if (campos.group(1).equals("id"))
                    campo += " PRIMARY KEY";
                columnas.add(campo);
            }
            if (tablas.group(1).equals("LIBRO")) {
                columnas.add("activo BOOLEAN GENERATED ALWAYS AS " + "(NOT eliminado AND NOT vetado_directamente AND numero_vetos = 0)");
            }
            Matcher unicos = Pattern.compile("(?m)^    UNIQUE KEY [^\\n]*").matcher(tablas.group(2));
            while (unicos.find()) {
                String unico = unicos.group().trim().replaceAll(",?$", "");
                if (!unico.contains("marca_") && !unico.contains("cupon_en_uso"))
                    columnas.add(unico);
            }
            try (var statement = conexion.createStatement()) {
                statement.execute("CREATE TABLE " + tablas.group(1) + " (" + String.join(",", columnas) + ")");
            }
        }
        manager = mockStatic(DBManager.class);
        manager.when(DBManager::getConnection).thenAnswer(inv -> DriverManager.getConnection(url));
        try (var statement = conexion.createStatement()) {
            statement.execute("INSERT INTO USUARIO (id,email,contrasenia,nombre,fecha_nacimiento,usuario_creacion)" + " VALUES (1,'ana@test','hash','Ana','2000-01-01','ana@test')," + " (2,'sin@test','hash','Sin compras','2001-01-01','sin@test')");
            statement.execute("INSERT INTO AUTOR (id,nombre,biografia,usuario_creacion) VALUES (1,'Autor','Bio','admin')");
            statement.execute("INSERT INTO CATEGORIA (id,nombre,usuario_creacion) VALUES (1,'Ficcion','admin')");
            statement.execute("INSERT INTO EDITORIAL (id,nombre,usuario_creacion) VALUES (1,'Editorial','admin')");
            statement.execute("INSERT INTO DIRECCION_ENVIO (id,id_usuario,alias,calle,distrito,ciudad,usuario_creacion)" + " VALUES (1,1,'Casa','Calle','Distrito','Lima','ana@test')," + " (2,1,'Trabajo','Otra','Distrito','Lima','ana@test')");
            statement.execute("INSERT INTO METODO_PAGO (id,id_usuario,titular,proveedor,tipo_metodo," + " numero_tarjeta_cifrado,tipo_tarjeta,fecha_vencimiento,usuario_creacion)" + " VALUES (1,1,'Ana','Banco','TARJETA_BANCARIA','cifrado','CREDITO','2030-01-01','ana@test')");
        }
    }

    @AfterEach
    void cerrarBase() throws Exception {
        TransactionContext.close();
        if (manager != null)
            manager.close();
        if (conexion != null) {
            try (var statement = conexion.createStatement()) {
                statement.execute("DROP ALL OBJECTS");
            }
            conexion.close();
        }
    }

    private Libro registrarLibro(String titulo, String idioma, String precio) throws Exception {
        Libro libro = new Libro();
        Autor autor = new Autor();
        autor.setId(1);
        Categoria categoria = new Categoria();
        categoria.setId(1);
        Editorial editorial = new Editorial();
        editorial.setId(1);
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setCategoria(categoria);
        libro.setEditorial(editorial);
        libro.setIdioma(idioma);
        libro.setIsbn(titulo);
        libro.setProveedor("Proveedor");
        libro.setSinopsis("Sinopsis");
        libro.setAnioEdicion(2026);
        libro.setNumeroPaginas(200);
        libro.setPrecioBase(new BigDecimal(precio));
        libro.setStockFisico(10);
        libro.setUsuarioCreacion("admin");
        libro.setFechaCreacion(FECHA);
        new LibroDAOImpl().insertar(libro);
        TransactionContext.commit();
        TransactionContext.close();
        return libro;
    }

    private Pedido registrarPedido(Libro libro) throws Exception {
        Pedido pedido = crearPedido(libro);
        TransactionContext.commit();
        TransactionContext.close();
        return pedido;
    }

    private Pedido crearPedido(Libro libro) throws Exception {
        BigDecimal unitario = new BigDecimal("30.00");
        BigDecimal porcentaje = libro.getPrecioBase().subtract(unitario).multiply(BigDecimal.valueOf(100)).divide(libro.getPrecioBase(), 2, RoundingMode.HALF_UP);
        return crearPedido(libro, unitario, porcentaje);
    }

    private Pedido crearPedido(Libro libro, BigDecimal unitario, BigDecimal porcentaje) throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setId(1);
        TarjetaBancaria metodo = new TarjetaBancaria();
        metodo.setId(1);
        Item item = new Item();
        item.setLibro(libro);
        item.setCantidad(2);
        item.setPrecioBase(libro.getPrecioBase());
        item.setPrecioUnitario(unitario);
        item.setPorcentajeDescuento(porcentaje);
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setDireccionEnvio(direccion);
        pedido.setMetodoPago(metodo);
        pedido.setItems(List.of(item));
        pedido.setSubtotalProductos(libro.getPrecioBase().multiply(new BigDecimal("2")));
        pedido.setDescuentoLibros(pedido.getSubtotalProductos().subtract(item.getSubTotal()));
        pedido.setTotal(item.getSubTotal());
        pedido.setCantidadTotalLibros(2);
        pedido.setFechaPago(FECHA);
        pedido.setEstado(EstadoPedido.EN_ALMACEN);
        pedido.setIdentificadorTransaccion(UUID.randomUUID().toString());
        pedido.setCodigoSeguimiento(UUID.randomUUID().toString());
        pedido.setFechaEntregaEstimada(FECHA.plusSeconds(90));
        pedido.setUsuarioCreacion("ana@test");
        pedido.setFechaCreacion(FECHA);
        new PedidoDAOImpl().insertar(pedido);
        ItemPedidoDAO items = new ItemPedidoDAOImpl();
        for (Item linea : pedido.getItems()) {
            linea.setUsuarioCreacion(pedido.getUsuarioCreacion());
            linea.setFechaCreacion(pedido.getFechaCreacion());
            items.insertar(pedido.getId(), linea);
        }
        return pedido;
    }

    @Test
    void filtraYOrdenaPrecioFinalAntesDePaginar() throws Exception {
        Libro caro = registrarLibro("Caro", "Español", "100.00");
        registrarLibro("Barato", "Ingles", "60.00");
        Descuento descuento = new Descuento();
        descuento.setLibro(caro);
        descuento.setPorcentaje(new BigDecimal("50"));
        descuento.setFechaInicio(FECHA.minusDays(1));
        descuento.setFechaFin(FECHA.plusDays(1));
        descuento.setUsuarioCreacion("admin");
        new DescuentoDAOImpl().insertar(descuento);
        TransactionContext.commit();
        TransactionContext.close();
        FiltroLibroDTO filtro = new FiltroLibroDTO(null, List.of(1, 9), List.of(1), List.of(1), List.of(), null, new BigDecimal("65"), true, false, OrdenLibro.PRECIO_ASCENDENTE, 1, 1);
        var resultado = new LibroDAOImpl().buscar(filtro, FECHA);
        assertEquals(2, resultado.totalResultados());
        assertEquals(caro.getId(), resultado.items().getFirst().id());
        assertEquals(0, new BigDecimal("50").compareTo(resultado.items().getFirst().precioConDescuento()));
    }

    @Test
    void cambioDeImagenEsIndependienteDeLaEdicionDeDatos() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        LibroDAO dao = new LibroDAOImpl();
        assertTrue(dao.cambiarImagen(libro.getId(), "https://example.test/imagen.png", "admin"));
        libro.setTitulo("Nuevo titulo");
        libro.setUsuarioActualizacion("admin");
        dao.modificar(libro);
        assertEquals("https://example.test/imagen.png", dao.leer(libro.getId()).getPortada());
        assertEquals(1, dao.leer(libro.getId()).getEditorial().getId());
        assertNull(dao.leer(libro.getId()).getEditorial().getNombre());
        assertTrue(dao.cambiarImagen(libro.getId(), null, "admin"));
        assertNull(dao.leer(libro.getId()).getPortada());
    }

    @Test
    void pedidoConservaImportesYNoCargaLibrosCompletos() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        Pedido pedido = registrarPedido(libro);
        PedidoDAO dao = new PedidoDAOImpl();
        Pedido leido = dao.leer(pedido.getId());
        assertEquals(1, leido.getItems().size());
        assertNull(leido.getItems().getFirst().getLibro().getTitulo());
        libro.setPrecioBase(new BigDecimal("99"));
        new LibroDAOImpl().cambiarPrecio(libro.getId(), libro.getPrecioBase(), "admin");
        var items = new ItemPedidoDAOImpl().obtenerItemsDTO(pedido.getId());
        assertEquals(0, new BigDecimal("50").compareTo(items.getFirst().precioOriginal()));
        assertEquals(0, new BigDecimal("30").compareTo(items.getFirst().precioUnitario()));
        assertEquals(1, dao.listarPorUsuario(1, FiltroPedidoDTO.porDefecto()).items().getFirst().portadas().size());
    }

    @Test
    void historialConservaPortadasDeCadaPedidoAlPaginar() throws Exception {
        Libro primero = registrarLibro("Primero", "Español", "50.00");
        Libro segundo = registrarLibro("Segundo", "Español", "60.00");
        LibroDAO libros = new LibroDAOImpl();
        libros.cambiarImagen(primero.getId(), "https://example.test/primero.png", "admin");
        libros.cambiarImagen(segundo.getId(), "https://example.test/segundo.png", "admin");
        Pedido anterior = registrarPedido(primero);
        Pedido reciente = registrarPedido(segundo);
        Item otraLinea = new Item(anterior.getItems().getFirst());
        otraLinea.setLibro(primero);
        new ItemPedidoDAOImpl().insertar(reciente.getId(), otraLinea);
        try (var sentencia = TransactionContext.getConnection().prepareStatement(
            "UPDATE PEDIDO SET subtotal_productos=220,descuento_libros=100,total=120," +
            " cantidad_total_libros=4 WHERE id=?")) {
            sentencia.setInt(1, reciente.getId());
            sentencia.executeUpdate();
        }
        TransactionContext.commit();
        TransactionContext.close();
        PedidoDAO pedidos = new PedidoDAOImpl();
        var primeraPagina = pedidos.listarPorUsuario(1, new FiltroPedidoDTO(List.of(), null, 1, 1));
        var segundaPagina = pedidos.listarPorUsuario(1, new FiltroPedidoDTO(List.of(), null, 2, 1));
        assertEquals(2, primeraPagina.totalResultados());
        assertEquals(reciente.getId(), primeraPagina.items().getFirst().pedido().id());
        assertEquals(4, primeraPagina.items().getFirst().pedido().cantidadLibros());
        assertEquals(List.of("https://example.test/segundo.png", "https://example.test/primero.png"),
            primeraPagina.items().getFirst().portadas());
        assertEquals(anterior.getId(), segundaPagina.items().getFirst().pedido().id());
        assertEquals(List.of("https://example.test/primero.png"), segundaPagina.items().getFirst().portadas());
        assertTrue(pedidos.listarPorUsuario(2, FiltroPedidoDTO.porDefecto()).items().isEmpty());

        anterior.setEstado(EstadoPedido.CANCELADO);
        anterior.setUsuarioActualizacion("ana@test");
        reciente.setEstado(EstadoPedido.EN_CAMINO);
        reciente.setUsuarioActualizacion("ana@test");
        assertTrue(pedidos.actualizarEstado(anterior, EstadoPedido.EN_ALMACEN));
        assertTrue(pedidos.actualizarEstado(reciente, EstadoPedido.EN_ALMACEN));
        TransactionContext.commit();
        TransactionContext.close();
        var estados = List.of(EstadoPedido.CANCELADO, EstadoPedido.EN_CAMINO);
        var ascendente = pedidos.listarPorUsuario(1, new FiltroPedidoDTO(estados, OrdenPedido.TOTAL_ASCENDENTE, 1, 1));
        var descendente = pedidos.listarPorUsuario(1, new FiltroPedidoDTO(estados, OrdenPedido.TOTAL_DESCENDENTE, 1, 1));
        assertEquals(2, ascendente.totalResultados());
        assertEquals(anterior.getId(), ascendente.items().getFirst().pedido().id());
        assertEquals(reciente.getId(), descendente.items().getFirst().pedido().id());
        assertEquals(1, pedidos.listarPorUsuario(1,
            new FiltroPedidoDTO(List.of(EstadoPedido.CANCELADO), OrdenPedido.TOTAL_ASCENDENTE, 1, 10)).totalResultados());
    }

    @Test
    void transaccionCompartidaPuedeRevertirse() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        TransactionContext.getConnection();
        assertTrue(new LibroDAOImpl().descontarStock(libro.getId(), 3, "ana@test"));
        assertFalse(new LibroDAOImpl().descontarStock(libro.getId(), 20, "ana@test"));
        TransactionContext.rollback();
        assertEquals(10, new LibroDAOImpl().leer(libro.getId()).getStockFisico());
        assertFalse(conexion.isClosed());
    }

    @Test
    void predeterminadoInvalidoNoBorraAnteriorYElValidoLoSustituye() throws Exception {
        TransactionContext.getConnection();
        DireccionEnvioDAO dao = new DireccionEnvioDAOImpl();
        assertTrue(dao.establecerPredeterminado(1, 1, "ana@test"));
        TransactionContext.commit();
        assertTrue(dao.leer(1).isPredeterminada());
        assertTrue(dao.establecerPredeterminado(1, 2, "ana@test"));
        TransactionContext.commit();
        assertFalse(dao.leer(1).isPredeterminada());
        assertTrue(dao.leer(2).isPredeterminada());
        assertFalse(dao.leer(2).isEliminado());
    }

    @Test
    void resenaUsaSoloLaImagenDeSuContextoYElVotoNoSeDuplica() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        new LibroDAOImpl().cambiarImagen(libro.getId(), "https://example.test/imagen.png", "admin");
        new UsuarioDAOImpl().cambiarImagen(1, "https://example.test/imagen.png", "ana@test");
        Resena resena = new Resena();
        resena.setLibro(libro);
        Usuario usuario = new Usuario();
        usuario.setId(1);
        resena.setUsuario(usuario);
        resena.setPuntuacion(4);
        resena.setComentario("Muy bueno");
        resena.setUsuarioCreacion("ana@test");
        ResenaDAO dao = new ResenaDAOImpl();
        dao.insertar(resena);
        var publica = dao.listar(libro.getId(), 0, 1, ConsultaResenasDTO.porDefecto()).items().getFirst();
        assertNotNull(publica.imagenUsuario());
        assertNull(publica.portadaLibro());
        var propia = dao.listar(0, 1, 1, ConsultaResenasDTO.porDefecto()).items().getFirst();
        assertNull(propia.imagenUsuario());
        assertNotNull(propia.portadaLibro());
        TransactionContext.getConnection();
        VotoUtilDAO votos = new VotoUtilDAOImpl();
        assertTrue(votos.cambiarVoto(2, resena.getId(), true, "sin@test"));
        dao.cambiarCantidadVotos(resena.getId(), 1, "sin@test");
        assertFalse(votos.cambiarVoto(2, resena.getId(), true, "sin@test"));
        TransactionContext.commit();
        assertEquals(1, dao.leer(resena.getId()).getCantidadVotosUtiles());
        assertTrue(votos.cambiarVoto(2, resena.getId(), false, "sin@test"));
        dao.cambiarCantidadVotos(resena.getId(), -1, "sin@test");
        assertFalse(votos.cambiarVoto(2, resena.getId(), false, "sin@test"));
        TransactionContext.commit();
        assertEquals(0, dao.leer(resena.getId()).getCantidadVotosUtiles());
        assertTrue(votos.cambiarVoto(2, resena.getId(), true, "sin@test"));
        dao.cambiarCantidadVotos(resena.getId(), 1, "sin@test");
        TransactionContext.commit();
        assertEquals(1, dao.leer(resena.getId()).getCantidadVotosUtiles());
        var votada = dao.listar(libro.getId(), 0, 2, ConsultaResenasDTO.porDefecto()).items().getFirst();
        assertTrue(votada.votoUtilPropio());
        try (var sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM VOTO_UTIL WHERE id_usuario=2 AND id_resena=?")) {
            sentencia.setInt(1, resena.getId());
            try (var resultado = sentencia.executeQuery()) {
                resultado.next();
                assertEquals(1, resultado.getInt(1));
            }
        }
        assertTrue(dao.eliminar(resena.getId(), "ana@test"));
        TransactionContext.commit();
        assertEquals(0, dao.listar(libro.getId(), 0, 2, ConsultaResenasDTO.porDefecto()).totalResultados());
        assertEquals(0, dao.obtenerValoracion(libro.getId()).cantidadResenas());
        assertTrue(dao.listarValoraciones().isEmpty());
    }

    @Test
    void solicitudResueltaNoSeEditaNiRetira() throws Exception {
        Pedido pedido = registrarPedido(registrarLibro("Libro", "Español", "50.00"));
        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        Usuario usuario = new Usuario();
        usuario.setId(1);
        solicitud.setUsuario(usuario);
        solicitud.setPedido(pedido);
        solicitud.setMotivo(MotivoDevolucion.LIBRO_DANADO);
        solicitud.setUsuarioCreacion("ana@test");
        SolicitudDAO dao = new SolicitudDAOImpl();
        dao.insertar(solicitud);
        TransactionContext.commit();
        TransactionContext.close();
        assertEquals(1, dao.listar(1, FiltroSolicitudDTO.porDefecto()).totalResultados());
        TransactionContext.getConnection();
        assertTrue(dao.resolver(solicitud.getId(), EstadoSolicitud.RECHAZADA, LocalDateTime.now(), "admin"));
        TransactionContext.commit();
        TransactionContext.close();
        assertFalse(dao.eliminar(solicitud.getId(), "ana@test"));
        solicitud.setComentario("Otro");
        solicitud.setUsuarioActualizacion("ana@test");
        assertThrows(java.sql.SQLException.class, () -> dao.modificar(solicitud));
        assertEquals(EstadoSolicitud.RECHAZADA, dao.leer(solicitud.getId()).getEstado());
    }

    @Test
    void reportesIncluyenInactivosSinComprasYExcluyenCancelados() throws Exception {
        Pedido pedido = registrarPedido(registrarLibro("Libro", "Español", "50.00"));
        LibroDAO libros = new LibroDAOImpl();
        UsuarioDAO usuariosDAO = new UsuarioDAOImpl();
        LocalDate dia = FECHA.toLocalDate();
        var ventas = libros.listarVentasLibros(new ConsultaReporteLibrosDTO(dia, dia, null));
        assertEquals(1, ventas.size());
        assertEquals("Editorial", ventas.getFirst().editorial());
        var usuarios = usuariosDAO.listarActividadUsuarios(dia, dia, null);
        assertEquals(2, usuarios.size());
        assertEquals("Ficcion", usuarios.getFirst().categoriaMasComprada().nombre());
        assertNull(usuarios.getLast().autorMasComprado());
        assertEquals(1, usuariosDAO.listarActividadUsuarios(dia, dia, 1).size());
        TransactionContext.getConnection();
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedido.setUsuarioActualizacion("ana@test");
        assertTrue(new PedidoDAOImpl().actualizarEstado(pedido, EstadoPedido.EN_ALMACEN));
        TransactionContext.commit();
        TransactionContext.close();
        assertTrue(libros.listarVentasLibros(new ConsultaReporteLibrosDTO(dia, dia, null)).isEmpty());
        assertEquals(0, usuariosDAO.listarActividadUsuarios(dia, dia, null).getFirst().totalGastado().signum());
    }

    @Test
    void detalleMuestraValoracionCompletaYCompraHabilitaResena() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        registrarPedido(libro);
        assertTrue(new PedidoDAOImpl().haCompradoLibro(1, libro.getId()));
        Resena resena = new Resena();
        resena.setLibro(libro);
        Usuario usuario = new Usuario();
        usuario.setId(1);
        resena.setUsuario(usuario);
        resena.setPuntuacion(5);
        resena.setComentario("Bueno");
        resena.setUsuarioCreacion("ana@test");
        ResenaDAO dao = new ResenaDAOImpl();
        dao.insertar(resena);
        var detalle = new LibroDAOImpl().obtenerDetalle(libro.getId(), 1, FECHA);
        assertTrue(dao.existePublicada(1, libro.getId()));
        assertEquals(1L, detalle.valoracion().cantidadCincoEstrellas());
        assertEquals(5.0, detalle.valoracion().promedio());

        assertTrue(dao.eliminar(resena.getId(), "ana@test"));
        TransactionContext.commit();
        TransactionContext.close();
        assertFalse(dao.existePublicada(1, libro.getId()));
        Resena nueva = new Resena(0, 3, "Segunda publicación", libro, usuario);
        nueva.setUsuarioCreacion("ana@test");
        dao.insertar(nueva);
        assertNotEquals(resena.getId(), nueva.getId());
        assertTrue(dao.leer(resena.getId()).isEliminado());
        assertTrue(dao.existePublicada(1, libro.getId()));
        assertEquals(nueva.getId(), dao.listar(libro.getId(), 0, 1, ConsultaResenasDTO.porDefecto()).items().getFirst().id());
        assertEquals(1, dao.obtenerValoracion(libro.getId()).cantidadResenas());
    }

    @Test
    void compraParaPublicarResenaDependeDelEstadoActualDelPedido() throws Exception {
        Libro libro = registrarLibro("Compra válida", "Español", "50.00");
        Pedido pedido = registrarPedido(libro);
        PedidoDAO dao = new PedidoDAOImpl();
        assertTrue(dao.haCompradoLibro(1, libro.getId()));
        assertFalse(dao.haCompradoLibro(2, libro.getId()));
        EstadoPedido anterior = EstadoPedido.EN_ALMACEN;
        for (EstadoPedido estado : List.of(EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO,
            EstadoPedido.CANCELADO, EstadoPedido.DEVUELTO)) {
            pedido.setEstado(estado);
            pedido.setUsuarioActualizacion("ana@test");
            assertTrue(dao.actualizarEstado(pedido, anterior));
            TransactionContext.commit();
            TransactionContext.close();
            assertEquals(estado == EstadoPedido.EN_CAMINO || estado == EstadoPedido.ENTREGADO,
                dao.haCompradoLibro(1, libro.getId()), estado.toString());
            anterior = estado;
        }
    }

    @Test
    void cuponSoloSeConsumeUnaVezYSeLiberaSiSigueVigente() throws Exception {
        Cupon cupon = new Cupon();
        Usuario usuario = new Usuario();
        usuario.setId(1);
        cupon.setUsuario(usuario);
        cupon.setPorcentaje(new BigDecimal("10"));
        cupon.setMontoMinimo(new BigDecimal("100"));
        cupon.setFechaInicio(FECHA.minusDays(1));
        cupon.setFechaFin(FECHA.plusDays(1));
        cupon.setUsuarioCreacion("sistema");
        CuponDAO dao = new CuponDAOImpl();
        dao.insertar(cupon);
        TransactionContext.getConnection();
        assertTrue(dao.consumir(cupon.getId(), 1, FECHA, "ana@test"));
        assertFalse(dao.consumir(cupon.getId(), 1, FECHA, "ana@test"));
        assertFalse(dao.liberar(cupon.getId(), FECHA.plusDays(2), "ana@test"));
        assertTrue(dao.liberar(cupon.getId(), FECHA, "ana@test"));
        TransactionContext.commit();
        assertFalse(dao.leer(cupon.getId()).isConsumido());
    }

    @Test
    void estadoEIntentosUsanCondicionesParaNoRepetirEfectos() throws Exception {
        Pedido pedido = registrarPedido(registrarLibro("Libro", "Español", "50.00"));
        PedidoDAO dao = new PedidoDAOImpl();
        TransactionContext.getConnection();
        pedido.setEstado(EstadoPedido.EN_CAMINO);
        pedido.setFechaSalida(FECHA.plusSeconds(30));
        pedido.setUsuarioActualizacion("ana@test");
        assertTrue(dao.actualizarEstado(pedido, EstadoPedido.EN_ALMACEN));
        assertFalse(dao.actualizarEstado(pedido, EstadoPedido.EN_ALMACEN));
        assertFalse(dao.incrementarIntentos(pedido.getId(), 3, "ana@test"));
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setFechaEntrega(FECHA.plusSeconds(90));
        assertTrue(dao.actualizarEstado(pedido, EstadoPedido.EN_CAMINO));
        assertTrue(dao.incrementarIntentos(pedido.getId(), 1, "ana@test"));
        assertFalse(dao.incrementarIntentos(pedido.getId(), 1, "ana@test"));
    }

    @Test
    void guardarYSeguirRecuperanLaMismaRelacion() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        LibroGuardadoDAO guardados = new LibroGuardadoDAOImpl();
        guardados.guardar(1, libro.getId(), false, "ana@test");
        guardados.guardar(1, libro.getId(), true, "ana@test");
        assertTrue(guardados.retirar(1, libro.getId(), false, "ana@test"));
        guardados.guardar(1, libro.getId(), false, "ana@test");
        var favoritos = new LibroDAOImpl().buscarGuardados(1, true, FiltroLibroDTO.porDefecto(), FECHA);
        assertEquals(1, favoritos.totalResultados());
        SeguimientoDAO seguidos = new SeguimientoDAOImpl();
        seguidos.guardar(1, TipoSeguimiento.AUTOR, 1, "ana@test");
        seguidos.guardar(1, TipoSeguimiento.AUTOR, 1, "ana@test");
        assertEquals(1, seguidos.listarPorUsuario(1, TipoSeguimiento.AUTOR, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12, "").totalResultados());
        assertTrue(new AutorDAOImpl().obtenerDetalle(1, 1).seguido());
    }

    @Test
    void reporteConservaOriginalAunqueCambieLaResena() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        Resena resena = new Resena();
        resena.setLibro(libro);
        Usuario usuario = new Usuario();
        usuario.setId(1);
        resena.setUsuario(usuario);
        resena.setComentario("Mensaje original");
        resena.setPuntuacion(2);
        resena.setUsuarioCreacion("ana@test");
        ResenaDAO resenas = new ResenaDAOImpl();
        resenas.insertar(resena);
        usuario.setId(2);
        SolicitudReporteResena reporte = new SolicitudReporteResena(0, usuario, resena, MotivoReporteResena.CONTENIDO_OFENSIVO, "Comentario");
        reporte.setUsuarioCreacion("sin@test");
        SolicitudDAO dao = new SolicitudDAOImpl();
        dao.insertar(reporte);
        TransactionContext.commit();
        TransactionContext.close();
        resena.setComentario("Corregido");
        resena.setUsuarioActualizacion("ana@test");
        resenas.modificar(resena);
        var detalle = dao.obtenerDetalleReporteResena(reporte.getId(), true);
        assertEquals("Mensaje original", detalle.resenaReportada().contenidoOriginal().comentario());
        assertEquals("Corregido", detalle.contenidoActual().comentario());
        assertTrue(dao.existeReportePendiente(2, resena.getId()));
        assertTrue(dao.eliminar(reporte.getId(), "sin@test"));
        assertEquals(0, dao.listar(2, new FiltroSolicitudDTO(me.Model.dto.solicitud.TipoSolicitud.REPORTE_RESENA, List.of(), 1, 10)).totalResultados());
    }

    @Test
    void recuperarMetodoPropioMantieneIdYNumeroCifrado() throws Exception {
        MetodoPagoDAO dao = new MetodoPagoDAOImpl();
        MetodoPago metodo = dao.leer(1);
        assertTrue(metodo instanceof TarjetaBancaria);
        assertEquals("cifrado", ((TarjetaBancaria) metodo).getNumeroTarjetaCifrado());
        assertTrue(dao.eliminar(1, "ana@test"));
        TransactionContext.getConnection();
        metodo.setUsuarioActualizacion("ana@test");
        assertTrue(dao.recuperar(metodo));
        TransactionContext.commit();
        assertEquals(1, dao.listarPorUsuario(1, 1, 10).totalResultados());
        assertFalse(dao.leer(1).isEliminado());
        assertTrue(dao.establecerPredeterminado(1, 1, FECHA.toLocalDate(), "ana@test"));
        TransactionContext.commit();
        assertTrue(dao.leer(1).isPredeterminado());
    }

    @Test
    void vetosSeAcumulanSinDuplicarseYSeRetiranPorSuOrigen() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        LibroDAO libros = new LibroDAOImpl();
        AutorDAO autores = new AutorDAOImpl();
        CategoriaDAO categorias = new CategoriaDAOImpl();
        assertTrue(autores.cambiarActivo(1, false, "admin"));
        libros.recalcularVetosPorAutor(1, "admin");
        assertFalse(autores.cambiarActivo(1, false, "admin"));
        libros.recalcularVetosPorAutor(1, "admin");
        assertTrue(categorias.cambiarActivo(1, false, "admin"));
        libros.recalcularVetosPorCategoria(1, "admin");
        assertTrue(autores.cambiarActivo(1, true, "admin"));
        libros.recalcularVetosPorAutor(1, "admin");
        TransactionContext.rollback();
        TransactionContext.close();
        assertTrue(libros.leer(libro.getId()).isActivo());
    }

    @Test
    void rollbackReviertePedidoItemsStockYCupoConsumido() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        Cupon cupon = new Cupon();
        Usuario usuario = new Usuario();
        usuario.setId(1);
        cupon.setUsuario(usuario);
        cupon.setPorcentaje(new BigDecimal("10"));
        cupon.setMontoMinimo(BigDecimal.ZERO);
        cupon.setFechaInicio(FECHA.minusDays(1));
        cupon.setFechaFin(FECHA.plusDays(1));
        cupon.setUsuarioCreacion("ana@test");
        CuponDAO cupones = new CuponDAOImpl();
        cupones.insertar(cupon);
        TransactionContext.commit();
        TransactionContext.close();
        TransactionContext.getConnection();
        assertTrue(new LibroDAOImpl().descontarStock(libro.getId(), 2, "ana@test"));
        assertTrue(cupones.consumir(cupon.getId(), 1, FECHA, "ana@test"));
        Pedido pedido = crearPedido(libro);
        assertNull(new PedidoDAOImpl().leer(pedido.getId()));
        TransactionContext.rollback();
        assertNull(new PedidoDAOImpl().leer(pedido.getId()));
        assertTrue(new ItemPedidoDAOImpl().obtenerItemsDTO(pedido.getId()).isEmpty());
        assertEquals(10, new LibroDAOImpl().leer(libro.getId()).getStockFisico());
        assertFalse(cupones.leer(cupon.getId()).isConsumido());
    }

    @Test
    void rollbackDeCancelacionConservaPedidoStockYCuponConsumido() throws Exception {
        Libro libro = registrarLibro("Cancelación", "Español", "50.00");
        Pedido pedido = registrarPedido(libro);
        PedidoDAO pedidos = new PedidoDAOImpl();
        LibroDAO libros = new LibroDAOImpl();
        CuponDAO cupones = new CuponDAOImpl();
        Cupon cupon = new Cupon(0, pedido.getUsuario(), BigDecimal.TEN, BigDecimal.ZERO,
            FECHA.minusDays(1), FECHA.plusDays(30));
        cupon.setUsuarioCreacion("ana@test");
        cupones.insertar(cupon);
        assertTrue(cupones.consumir(cupon.getId(), 1, FECHA, "ana@test"));
        assertTrue(libros.descontarStock(libro.getId(), 2, "ana@test"));
        try (var sentencia = TransactionContext.getConnection().prepareStatement(
            "UPDATE PEDIDO SET id_cupon=?,porcentaje_cupon=10,descuento_cupon=6,total=54 WHERE id=?")) {
            sentencia.setInt(1, cupon.getId());
            sentencia.setInt(2, pedido.getId());
            sentencia.executeUpdate();
        }
        TransactionContext.commit();
        TransactionContext.close();

        pedido.setEstado(EstadoPedido.CANCELADO);
        pedido.setUsuarioActualizacion("ana@test");
        assertTrue(pedidos.actualizarEstado(pedido, EstadoPedido.EN_ALMACEN));
        assertTrue(libros.reponerStock(libro.getId(), 2, "ana@test"));
        assertTrue(cupones.liberar(cupon.getId(), FECHA, "ana@test"));
        TransactionContext.rollback();
        TransactionContext.close();
        assertEquals(EstadoPedido.EN_ALMACEN, pedidos.leer(pedido.getId()).getEstado());
        assertEquals(8, libros.leer(libro.getId()).getStockFisico());
        assertTrue(cupones.leer(cupon.getId()).isConsumido());
        assertEquals(0, new BigDecimal("54.00").compareTo(pedidos.leer(pedido.getId()).getTotal()));
    }

    @Test
    void rollbackDePrimeraEntregaRevierteEstadoContadorYPremio() throws Exception {
        Pedido pedido = registrarPedido(registrarLibro("Entrega", "Español", "50.00"));
        PedidoDAO pedidos = new PedidoDAOImpl();
        UsuarioDAO usuarios = new UsuarioDAOImpl();
        CuponDAO cupones = new CuponDAOImpl();
        pedido.setEstado(EstadoPedido.EN_CAMINO);
        pedido.setFechaSalida(FECHA.plusSeconds(30));
        pedido.setUsuarioActualizacion("ana@test");
        assertTrue(pedidos.actualizarEstado(pedido, EstadoPedido.EN_ALMACEN));
        try (var sentencia = TransactionContext.getConnection().createStatement()) {
            sentencia.executeUpdate("UPDATE USUARIO SET cantidad_pedidos_entregados=4 WHERE id=1");
        }
        TransactionContext.commit();
        TransactionContext.close();

        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setFechaEntrega(FECHA.plusSeconds(90));
        assertTrue(pedidos.actualizarEstado(pedido, EstadoPedido.EN_CAMINO));
        assertTrue(usuarios.incrementarPedidosEntregados(1, "ana@test"));
        Cupon cupon = new Cupon(0, pedido.getUsuario(), BigDecimal.TEN, new BigDecimal("100"),
            pedido.getFechaEntrega(), pedido.getFechaEntrega().plusDays(30));
        cupon.setUsuarioCreacion("ana@test");
        cupones.insertar(cupon);
        TransactionContext.rollback();
        TransactionContext.close();
        assertEquals(EstadoPedido.EN_CAMINO, pedidos.leer(pedido.getId()).getEstado());
        assertNull(pedidos.leer(pedido.getId()).getFechaEntrega());
        assertEquals(4, usuarios.leer(1).getCantidadPedidosEntregados());
        assertNull(cupones.leer(cupon.getId()));
    }

    @Test
    void vigenciasYSolapamientoIncluyenLimitesYSinEscriturasAlLeer() throws Exception {
        Libro libro = registrarLibro("Libro", "Español", "50.00");
        Descuento descuento = new Descuento();
        descuento.setLibro(libro);
        descuento.setPorcentaje(new BigDecimal("20"));
        descuento.setFechaInicio(FECHA);
        descuento.setFechaFin(FECHA.plusDays(1));
        descuento.setActivo(false);
        descuento.setUsuarioCreacion("admin");
        DescuentoDAO dao = new DescuentoDAOImpl();
        dao.insertar(descuento);
        TransactionContext.commit();
        TransactionContext.close();
        try (var statement = conexion.createStatement()) {
            statement.executeUpdate("UPDATE DESCUENTO SET activo = FALSE");
        }
        assertFalse(dao.leer(descuento.getId()).isActivo());
        assertNotNull(dao.obtenerVigente(libro.getId(), FECHA));
        TransactionContext.getConnection();
        assertTrue(dao.haySuperposicion(libro.getId(), 0, FECHA.plusDays(1), FECHA.plusDays(2)));
        assertFalse(dao.haySuperposicion(libro.getId(), descuento.getId(), FECHA, FECHA.plusDays(2)));
        descuento.setFechaFin(FECHA.plusDays(2));
        descuento.setUsuarioActualizacion("admin");
        descuento.setActivo(true);
        dao.modificar(descuento);
        TransactionContext.commit();
        assertTrue(dao.leer(descuento.getId()).isActivo());
        TransactionContext.commit();
        TransactionContext.close();
        assertNull(dao.obtenerVigente(libro.getId(), FECHA.plusDays(3)));
    }

    @Test
    void metodoVencidoNoSustituyePredeterminadoYElEventoQuitaSuMarca() throws Exception {
        MetodoPagoDAO dao = new MetodoPagoDAOImpl();
        TarjetaBancaria tarjeta = new TarjetaBancaria();
        Usuario usuario = new Usuario();
        usuario.setId(1);
        tarjeta.setUsuario(usuario);
        tarjeta.setTitular("Ana");
        tarjeta.setProveedor("Banco");
        tarjeta.setNumeroTarjetaCifrado("otro cifrado");
        tarjeta.setTipoTarjeta(TipoTarjeta.CREDITO);
        tarjeta.setFechaVencimiento(FECHA.toLocalDate().minusDays(1));
        tarjeta.setUsuarioCreacion("ana@test");
        dao.insertar(tarjeta);
        TransactionContext.getConnection();
        assertTrue(dao.establecerPredeterminado(1, 1, FECHA.toLocalDate(), "ana@test"));
        assertFalse(dao.establecerPredeterminado(1, tarjeta.getId(), FECHA.toLocalDate(), "ana@test"));
        TransactionContext.commit();
        TransactionContext.close();
        assertTrue(dao.leer(tarjeta.getId()).isActivo());
        dao.desactivarVencidos(FECHA.toLocalDate(), "ana@test");
        assertFalse(dao.leer(tarjeta.getId()).isActivo());
        assertEquals(2, dao.listarPorUsuario(1, 1, 10).totalResultados());
        dao.desactivarVencidos(LocalDate.of(2031, 1, 1), "ana@test");
        assertFalse(dao.leer(1).isPredeterminado());
    }

    @Test
    void recomendacionesLeenLibrosActivosYTarjetasConservanElOrdenSolicitado() throws Exception {
        Libro primero = registrarLibro("Primero", "Español", "50.00");
        Libro segundo = registrarLibro("Segundo", "Español", "60.00");
        LibroDAO dao = new LibroDAOImpl();
        dao.cambiarImagen(primero.getId(), "https://example.test/imagen.png", "admin");
        var candidatos = dao.listarParaRecomendaciones();
        assertEquals("https://example.test/imagen.png", candidatos.getFirst().getPortada());
        assertEquals(1, candidatos.getFirst().getAutor().getId());
        var tarjetas = dao.listarTarjetasPorIds(List.of(segundo.getId(), primero.getId()), FECHA);
        assertEquals(segundo.getId(), tarjetas.getFirst().id());
        assertNotNull(tarjetas.getLast().portada());
        dao.cambiarVetoDirecto(segundo.getId(), true, "admin");
        assertEquals(1, dao.listarTarjetasPorIds(List.of(segundo.getId(), primero.getId()), FECHA).size());
        assertTrue(dao.listarTarjetasPorIds(List.of(), FECHA).isEmpty());
    }

    @Test
    void historialDeRecomendacionesIncluyeLosTresEstadosValidosSinContarCanceladosNiDevueltos() throws Exception {
        Libro libro = registrarLibro("Historial", "Español", "50.00");
        PedidoDAO dao = new PedidoDAOImpl();
        List<Integer> idsPedidos = new ArrayList<>();
        for (EstadoPedido estado : List.of(EstadoPedido.EN_ALMACEN, EstadoPedido.EN_CAMINO,
            EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO, EstadoPedido.DEVUELTO)) {
            Pedido pedido = registrarPedido(libro);
            idsPedidos.add(pedido.getId());
            if (estado != EstadoPedido.EN_ALMACEN) {
                pedido.setEstado(estado);
                pedido.setUsuarioActualizacion("ana@test");
                assertTrue(dao.actualizarEstado(pedido, EstadoPedido.EN_ALMACEN));
                TransactionContext.commit();
                TransactionContext.close();
            }
        }

        var ventas = dao.listarDetalleVentas(null);
        assertEquals(idsPedidos.subList(0, 3), ventas.stream().map(venta -> venta.idPedido()).toList());
        assertEquals(List.of(2, 2, 2), ventas.stream().map(venta -> venta.cantidad()).toList());
        assertTrue(dao.listarDetalleVentas(FECHA.toLocalDate().plusDays(1)).isEmpty());
    }

    @Test
    void referenciasSeguimientosYUsuarioConservanIdentidadYAuditoriaDeCreacion() throws Exception {
        Categoria categoria = new Categoria();
        categoria.setNombre("Otra categoria");
        categoria.setDescripcion("Descripcion");
        categoria.setUsuarioCreacion("admin");
        CategoriaDAO categorias = new CategoriaDAOImpl();
        categorias.insertar(categoria);
        categorias.cambiarImagen(categoria.getId(), "https://example.test/imagen.png", "admin");
        categoria.setNombre("Categoria editada");
        categoria.setUsuarioCreacion("otro creador");
        categoria.setUsuarioActualizacion("admin");
        categorias.modificar(categoria);
        assertEquals("admin", categorias.leer(categoria.getId()).getUsuarioCreacion());
        assertEquals("https://example.test/imagen.png", categorias.leer(categoria.getId()).getImagen());
        SeguimientoDAO seguidos = new SeguimientoDAOImpl();
        seguidos.guardar(1, TipoSeguimiento.CATEGORIA, categoria.getId(), "ana@test");
        assertTrue(seguidos.retirar(1, TipoSeguimiento.CATEGORIA, categoria.getId(), "ana@test"));
        seguidos.guardar(1, TipoSeguimiento.CATEGORIA, categoria.getId(), "ana@test");
        assertEquals(1, seguidos.listarPorUsuario(1, TipoSeguimiento.CATEGORIA, OrdenReferencia.MAS_RECIENTES, 1, 12, "").totalResultados());
        SeguimientoDAO editoriales = new SeguimientoDAOImpl();
        editoriales.guardar(1, TipoSeguimiento.EDITORIAL, 1, "ana@test");
        editoriales.guardar(1, TipoSeguimiento.EDITORIAL, 1, "ana@test");
        assertEquals(1, editoriales.listarPorUsuario(1, TipoSeguimiento.EDITORIAL, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12, "").totalResultados());
        UsuarioDAO usuarios = new UsuarioDAOImpl();
        Usuario usuario = usuarios.leer(1);
        usuario.setNombre("Nombre completo");
        usuario.setEmail("correo cambiado");
        usuario.setContrasenia("otra clave");
        usuario.setRol(Rol.ADMIN);
        usuario.setUsuarioActualizacion("ana@test");
        usuarios.modificar(usuario);
        Usuario actualizado = usuarios.leer(1);
        assertEquals("Nombre completo", actualizado.getNombre());
        assertEquals("ana@test", actualizado.getEmail());
        assertEquals("hash", actualizado.getContrasenia());
        assertEquals(Rol.CLIENTE, actualizado.getRol());
    }

    @Test
    void rollbackDeModeracionRevierteResolucionYRetiroDeResena() throws Exception {
        Libro libro = registrarLibro("Libro reportado", "Español", "50.00");
        Usuario autor = new Usuario();
        autor.setId(1);
        Resena resena = new Resena(0, 2, "Contenido reportado", libro, autor);
        resena.setUsuarioCreacion("ana@test");
        ResenaDAO resenas = new ResenaDAOImpl();
        resenas.insertar(resena);
        Usuario solicitante = new Usuario();
        solicitante.setId(2);
        SolicitudReporteResena reporte = new SolicitudReporteResena(0, solicitante, resena, MotivoReporteResena.CONTENIDO_OFENSIVO, "Revisar contenido");
        reporte.setUsuarioCreacion("sin@test");
        SolicitudDAO solicitudes = new SolicitudDAOImpl();
        solicitudes.insertar(reporte);
        TransactionContext.commit();
        TransactionContext.close();
        assertTrue(solicitudes.resolver(reporte.getId(), EstadoSolicitud.ACEPTADA, LocalDateTime.now(), "admin"));
        assertTrue(resenas.eliminar(resena.getId(), "admin"));
        TransactionContext.rollback();
        TransactionContext.close();
        assertEquals(EstadoSolicitud.PENDIENTE, solicitudes.leer(reporte.getId()).getEstado());
        assertNull(solicitudes.leer(reporte.getId()).getFechaResolucion());
        assertTrue(resenas.leer(resena.getId()).estaDisponible());
        assertTrue(solicitudes.existeReportePendiente(2, resena.getId()));
        assertEquals(1, resenas.obtenerValoracion(libro.getId()).cantidadResenas());
        assertTrue(solicitudes.resolver(reporte.getId(), EstadoSolicitud.ACEPTADA, LocalDateTime.now(), "admin"));
        assertTrue(resenas.eliminar(resena.getId(), "admin"));
        TransactionContext.commit();
        TransactionContext.close();
        assertEquals(0, resenas.obtenerValoracion(libro.getId()).cantidadResenas());
    }

    @Test
    void devolucionPendienteSeConsultaYNoSeCierraComoReporte() throws Exception {
        Pedido pedido = registrarPedido(registrarLibro("Libro para devolver", "Español", "50.00"));
        PedidoDAO pedidos = new PedidoDAOImpl();
        pedido.setUsuarioActualizacion("ana@test");
        pedido.setEstado(EstadoPedido.EN_CAMINO);
        pedido.setFechaSalida(FECHA.plusSeconds(30));
        assertTrue(pedidos.actualizarEstado(pedido, EstadoPedido.EN_ALMACEN));
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setFechaEntrega(FECHA.plusSeconds(90));
        assertTrue(pedidos.actualizarEstado(pedido, EstadoPedido.EN_CAMINO));
        assertTrue(pedidos.incrementarIntentos(pedido.getId(), 3, "ana@test"));
        SolicitudDevolucion primera = new SolicitudDevolucion(0, pedido.getUsuario(), pedido, MotivoDevolucion.LIBRO_DANADO, "Portada dañada");
        primera.setUsuarioCreacion("ana@test");
        SolicitudDAO solicitudes = new SolicitudDAOImpl();
        solicitudes.insertar(primera);
        assertFalse(pedidos.incrementarIntentos(pedido.getId(), 3, "ana@test"));
        assertFalse(solicitudes.resolver(primera.getId(), EstadoSolicitud.CERRADA, LocalDateTime.now(), "admin"));
        TransactionContext.commit();
        TransactionContext.close();
        assertEquals(1, solicitudes.listar(1, FiltroSolicitudDTO.porDefecto()).totalResultados());
        assertEquals(EstadoSolicitud.PENDIENTE, solicitudes.leer(primera.getId()).getEstado());
        assertNull(solicitudes.leer(primera.getId()).getFechaResolucion());
        assertEquals(1, pedidos.leer(pedido.getId()).getNumeroIntentos());
    }

    @Test
    void itemHistoricoConservaCobroYOmiteDisponibilidadActual() throws Exception {
        Libro libro = registrarLibro("Libro historico", "Español", "50.00");
        Pedido pedido = registrarPedido(libro);
        new LibroDAOImpl().cambiarPrecio(libro.getId(), new BigDecimal("99.00"), "admin");
        ItemPedidoDAO items = new ItemPedidoDAOImpl();
        var detalle = items.obtenerItemsDTO(pedido.getId()).getFirst();
        assertEquals(0, new BigDecimal("50.00").compareTo(detalle.precioOriginal()));
        assertEquals(0, new BigDecimal("30.00").compareTo(detalle.precioUnitario()));
        assertEquals(0, new BigDecimal("40.00").compareTo(detalle.porcentajeDescuento()));
        assertEquals(0, new BigDecimal("60.00").compareTo(detalle.subtotal()));
        assertEquals(2, detalle.cantidad());
        assertNull(detalle.activo());
        assertNull(detalle.stockDisponible());
        Item entidad = items.listarPorPedido(pedido.getId()).getFirst();
        assertNull(entidad.getLibro().getTitulo());
        assertEquals(libro.getId(), entidad.getLibro().getId());
        assertEquals(0, new BigDecimal("50.00").compareTo(entidad.getPrecioBase()));
        assertEquals("ana@test", entidad.getUsuarioCreacion());
    }

    @Test
    void porcentajeNominalSeConservaAunqueElPrecioUnitarioEsteRedondeado() throws Exception {
        Libro libro = registrarLibro("Precio con centavos", "Español", "19.99");
        // 19.99 * 0.85 = 16.9915: el porcentaje aplicado es 15%, aunque el cobro sea 16.99.
        Pedido pedido = crearPedido(libro, new BigDecimal("16.99"), new BigDecimal("15.00"));
        TransactionContext.commit();
        TransactionContext.close();
        new LibroDAOImpl().cambiarPrecio(libro.getId(), new BigDecimal("29.99"), "admin");
        ItemPedidoDAO items = new ItemPedidoDAOImpl();
        var dto = items.obtenerItemsDTO(pedido.getId()).getFirst();
        assertEquals(0, new BigDecimal("19.99").compareTo(dto.precioOriginal()));
        assertEquals(0, new BigDecimal("16.99").compareTo(dto.precioUnitario()));
        assertEquals(0, new BigDecimal("15.00").compareTo(dto.porcentajeDescuento()));
        Item historico = items.listarPorPedido(pedido.getId()).getFirst();
        assertEquals(0, new BigDecimal("15.00").compareTo(historico.getPorcentajeDescuento()));
        Item copia = new Item(historico);
        assertEquals(historico.getPorcentajeDescuento(), copia.getPorcentajeDescuento());
    }
}
