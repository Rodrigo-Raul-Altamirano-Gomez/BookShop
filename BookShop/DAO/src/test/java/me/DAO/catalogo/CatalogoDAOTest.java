package me.DAO.catalogo;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.dto.catalogo.DetalleLibroDTO;
import me.Model.dto.catalogo.EstadoDescuento;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.FiltroReferenciaDTO;
import me.Model.dto.catalogo.OrdenLibro;
import me.Model.dto.catalogo.OrdenReferencia;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.catalogo.Libro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogoDAOTest {
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 6, 12, 0);
    private final LibroDAO libros = new LibroDAOImpl();
    private final AutorDAO autores = new AutorDAOImpl();
    private final CategoriaDAO categorias = new CategoriaDAOImpl();
    private final EditorialDAO editoriales = new EditorialDAOImpl();
    private final DescuentoDAO descuentos = new DescuentoDAOImpl();
    private MockedStatic<DBManager> manager;
    private final List<Connection> prestamos = new ArrayList<>();
    private Connection conexion;
    private String url;
    private Libro primero;
    private Libro segundo;
    private Libro tercero;

    @BeforeEach
    void preparar() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        conexion = DriverManager.getConnection(url);
        // Adaptacion de columnas del DDL vigente para probar SQL y mapeos en H2.
        // No sustituye una prueba de las restricciones y particularidades del motor MySQL.
        Path ddl = Path.of("../../BookShop/DAO/src/main/resources/DDL.sql");
        if (!Files.exists(ddl)) ddl = Path.of("src/main/resources/DDL.sql");
        String esquema = Files.readString(ddl).replaceAll(",\\r?\\n {8,}", ", ");
        Matcher tablas = Pattern.compile("CREATE TABLE (\\w+) \\((.*?)\\) ENGINE", Pattern.DOTALL)
                .matcher(esquema);
        while (tablas.find()) {
            List<String> columnas = new ArrayList<>();
            Matcher campos = Pattern.compile("(?m)^    ([a-z_]+) (INT|BIGINT|TINYINT|VARCHAR|TEXT|MEDIUMBLOB|"
                    + "DATETIME|DATE|BOOLEAN|DECIMAL|ENUM)\\b[^\\n]*").matcher(tablas.group(2));
            while (campos.find()) {
                String campo = campos.group().trim().replaceAll(",?$", "")
                        .replaceAll(" COMMENT '[^']*'", "");
                if (campo.contains("GENERATED")) continue;
                if (campos.group(2).equals("ENUM")) {
                    campo = campo.replaceAll("ENUM\\([^)]*\\)", "VARCHAR(40)");
                }
                if (campos.group(1).equals("id")) campo += " PRIMARY KEY";
                columnas.add(campo);
            }
            if (tablas.group(1).equals("LIBRO")) {
                columnas.add("activo BOOLEAN GENERATED ALWAYS AS "
                        + "(NOT eliminado AND NOT vetado_directamente AND numero_vetos = 0)");
            }
            try (var statement = conexion.createStatement()) {
                statement.execute("CREATE TABLE " + tablas.group(1) + " (" + String.join(",", columnas) + ")");
            }
        }
        manager = mockStatic(DBManager.class);
        manager.when(DBManager::getConnection).thenAnswer(inv -> {
            Connection prestamo = DriverManager.getConnection(url);
            prestamos.add(prestamo);
            return prestamo;
        });
        ejecutar("INSERT INTO USUARIO (id,email,contrasenia,nombre,fecha_nacimiento,usuario_creacion)"
                + " VALUES (1,'ana@test','hash','Ana','2000-01-01','ana@test'),"
                + " (2,'luis@test','hash','Luis','2000-01-01','luis@test')");
        for (int id = 1; id <= 2; id++) {
            Autor autor = new Autor();
            autor.setNombre("Autor " + id);
            autor.setBiografia("Biografia " + id);
            autor.setUsuarioCreacion("admin@admin.bookshop");
            autor.setFechaCreacion(FECHA.minusDays(id));
            autores.insertar(autor);
            Categoria categoria = new Categoria();
            categoria.setNombre("Categoria " + id);
            categoria.setDescripcion("Descripcion " + id);
            categoria.setUsuarioCreacion("admin@admin.bookshop");
            categoria.setFechaCreacion(FECHA.minusDays(id));
            categorias.insertar(categoria);
            Editorial editorial = new Editorial();
            editorial.setNombre("Editorial " + id);
            editorial.setDescripcion("Descripcion " + id);
            editorial.setUsuarioCreacion("admin@admin.bookshop");
            editorial.setFechaCreacion(FECHA.minusDays(id));
            editoriales.insertar(editorial);
        }
        primero = registrarLibro("Alfa", 1, "Español", "100.01", 100, 10, 2020, 1);
        segundo = registrarLibro("Beta", 2, "Inglés", "70.00", 300, 30, 2024, 2);
        tercero = registrarLibro("Gamma", 1, "Español", "90.00", 200, 20, 2022, 3);
        TransactionContext.commit();
        TransactionContext.close();
    }

    @AfterEach
    void cerrar() throws Exception {
        TransactionContext.rollback();
        TransactionContext.close();
        if (manager != null) manager.close();
        if (conexion != null) conexion.close();
        for (Connection prestamo : prestamos) assertTrue(prestamo.isClosed(), "Conexion sin cerrar");
    }

    @Test
    void combinaGruposYCuentaAntesDePaginar() throws Exception {
        registrarDescuento(promocion(primero, "50", FECHA.minusDays(1), FECHA.plusDays(1)));
        FiltroLibroDTO filtro = new FiltroLibroDTO(null, List.of(1, 2), List.of(1), List.of(1),
                List.of("Español", "Inglés"), new BigDecimal("50.01"), new BigDecimal("90.00"),
                true, false, OrdenLibro.PRECIO_ASCENDENTE, 1, 1);
        ResultadoPaginadoDTO<TarjetaLibroDTO> pagina = libros.buscar(filtro, FECHA);
        assertEquals(2, pagina.totalResultados());
        assertEquals(List.of(primero.getId()), ids(pagina));
        assertEquals(new BigDecimal("50.01"), pagina.items().getFirst().precioConDescuento());
        FiltroLibroDTO siguiente = new FiltroLibroDTO(filtro.titulo(), filtro.idsCategorias(),
                filtro.idsAutores(), filtro.idsEditoriales(), filtro.idiomas(), filtro.precioMinimo(),
                filtro.precioMaximo(), true, false, filtro.orden(), 2, 1);
        assertEquals(List.of(tercero.getId()), ids(libros.buscar(siguiente, FECHA)));
    }

    @ParameterizedTest
    @EnumSource(value = OrdenLibro.class, names = "RECOMENDADOS_PRIMERO", mode = EnumSource.Mode.EXCLUDE)
    void ordenaTodoElConjuntoAntesDePaginar(OrdenLibro orden) throws Exception {
        registrarDescuento(promocion(primero, "50", FECHA.minusDays(1), FECHA.plusDays(1)));
        registrarVenta(primero, 1, "EN_ALMACEN", 2, "40.00", FECHA.minusDays(2));
        registrarVenta(segundo, 1, "ENTREGADO", 1, "70.00", FECHA.minusDays(1));
        registrarVenta(tercero, 1, "CANCELADO", 99, "90.00", FECHA);
        registrarVenta(tercero, 1, "DEVUELTO", 99, "90.00", FECHA);
        registrarVenta(tercero, 1, "ENTREGADO", 99, "90.00", FECHA.minusDays(31));
        registrarVenta(tercero, 1, "EN_ALMACEN", 99, "90.00", FECHA.plusDays(1));
        Map<OrdenLibro, List<Integer>> esperados = Map.of(
                OrdenLibro.MAS_RECIENTES, List.of(1, 2, 3),
                OrdenLibro.MAS_VENDIDOS, List.of(1, 2, 3),
                OrdenLibro.PRECIO_ASCENDENTE, List.of(1, 2, 3),
                OrdenLibro.PRECIO_DESCENDENTE, List.of(3, 2, 1),
                OrdenLibro.TITULO_ASCENDENTE, List.of(1, 2, 3),
                OrdenLibro.PAGINAS_ASCENDENTE, List.of(1, 3, 2),
                OrdenLibro.PAGINAS_DESCENDENTE, List.of(2, 3, 1),
                OrdenLibro.STOCK_DESCENDENTE, List.of(2, 3, 1),
                OrdenLibro.ANIO_EDICION_DESCENDENTE, List.of(2, 3, 1));
        FiltroLibroDTO filtro = filtro(orden, true, 2, 1);
        assertEquals(List.of(esperados.get(orden).get(1)), ids(libros.buscar(filtro, FECHA)));
    }

    @Test
    void tituloSoloBuscaTituloYTrataComodinesComoTexto() throws Exception {
        assertEquals(0, libros.buscar(conTitulo("Autor"), FECHA).totalResultados());
        assertEquals(List.of(1), ids(libros.buscar(conTitulo("  lf  "), FECHA)));
        registrarLibro("Un 10%_especial!", 1, "Español", "20", 100, 1, 2026, 0);
        assertEquals(1, libros.buscar(conTitulo("%_"), FECHA).totalResultados());
        assertEquals(0, libros.buscar(conTitulo("' OR 1=1 --"), FECHA).totalResultados());
    }

    @Test
    void activosTodosYBorradoLogicoSonDistintos() throws Exception {
        libros.cambiarVetoDirecto(1, true, "admin");
        libros.eliminar(2, "admin");
        assertEquals(List.of(3), ids(libros.buscar(new FiltroLibroDTO(), FECHA)));
        var todos = libros.buscar(filtro(OrdenLibro.TITULO_ASCENDENTE, false, 1, 12), FECHA);
        assertEquals(List.of(1, 3), ids(todos));
        assertFalse(todos.items().getFirst().activo());
        assertTrue(libros.leer(2).isEliminado());
        assertNull(libros.obtenerDetalle(2, 1, FECHA));
        assertFalse(libros.eliminar(2, "admin"));
    }

    @Test
    void paginasDeReferenciaMantienenSuAmbitoAunqueElFiltroIntenteCambiarlo() throws Exception {
        FiltroLibroDTO categoria = new FiltroLibroDTO(null, List.of(2), List.of(), List.of(), List.of(),
                null, null, true, false, OrdenLibro.TITULO_ASCENDENTE, 1, 12);
        FiltroLibroDTO autor = new FiltroLibroDTO(null, List.of(), List.of(2), List.of(), List.of(),
                null, null, true, false, OrdenLibro.TITULO_ASCENDENTE, 1, 12);
        FiltroLibroDTO editorial = new FiltroLibroDTO(null, List.of(), List.of(), List.of(2), List.of(),
                null, null, true, false, OrdenLibro.TITULO_ASCENDENTE, 1, 12);
        assertEquals(List.of(1, 3), ids(libros.buscarPorCategoria(1, categoria, FECHA)));
        assertEquals(List.of(1, 3), ids(libros.buscarPorAutor(1, autor, FECHA)));
        assertEquals(List.of(1, 3), ids(libros.buscarPorEditorial(1, editorial, FECHA)));
    }

    @Test
    void listasPersonalesSonIndependientesYConservanFiltros() throws Exception {
        guardar(1, 1, true);
        guardar(1, 1, false);
        guardar(1, 3, false);
        guardar(2, 2, true);
        assertEquals(List.of(1), ids(libros.buscarGuardados(1, true, new FiltroLibroDTO(), FECHA)));
        assertEquals(List.of(1, 3), ids(libros.buscarGuardados(1, false, new FiltroLibroDTO(), FECHA)));
        assertEquals(List.of(2), ids(libros.buscarGuardados(2, true, new FiltroLibroDTO(), FECHA)));
        assertEquals(List.of(3), ids(libros.buscarGuardados(1, false, conTitulo("Gamma"), FECHA)));
        assertThrows(IllegalArgumentException.class,
                () -> libros.buscarGuardados(0, true, new FiltroLibroDTO(), FECHA));
    }

    @Test
    void nuevosUsaCreacionConLimitesDeTreintaDias() throws Exception {
        ejecutar("UPDATE LIBRO SET fecha_creacion = ? WHERE id = 1", FECHA.minusDays(30));
        ejecutar("UPDATE LIBRO SET fecha_creacion = ? WHERE id = 2", FECHA.minusDays(30).minusSeconds(1));
        ejecutar("UPDATE LIBRO SET fecha_creacion = ? WHERE id = 3", FECHA.plusSeconds(1));
        FiltroLibroDTO nuevos = new FiltroLibroDTO(null, null, null, null, null, null, null,
                true, true, OrdenLibro.MAS_RECIENTES, 1, 12);
        assertEquals(List.of(1), ids(libros.buscar(nuevos, FECHA)));
    }

    @Test
    void idiomasTieneBusquedaPaginacionYNoUnLimiteOculto() throws Exception {
        assertEquals(2, libros.buscarIdiomas(null, true, 1, 1).totalResultados());
        assertEquals(List.of("Inglés"), libros.buscarIdiomas(null, true, 2, 1).items());
        libros.cambiarVetoDirecto(2, true, "admin");
        assertEquals(List.of(), libros.buscarIdiomas("Ingl", true, 1, 12).items());
        assertEquals(List.of("Inglés"), libros.buscarIdiomas("Ingl", false, 1, 12).items());
    }

    @Test
    void recomendacionesNoCargaPortadasNiRecortaElConjuntoYRespetaOrdenDeIds() throws Exception {
        libros.cambiarImagen(1, new ImagenDTO(new byte[]{1, 2}, "image/png"), "admin");
        for (int i = 0; i < 9; i++) {
            registrarLibro("Extra " + i, 1, "Español", "10", 100, 1, 2026, 0);
        }
        libros.cambiarVetoDirecto(2, true, "admin");
        libros.eliminar(3, "admin");
        List<Libro> candidatos = libros.listarParaRecomendaciones();
        assertEquals(10, candidatos.size());
        assertNull(candidatos.getFirst().getPortada());
        assertNull(candidatos.getFirst().getAutor().getNombre());
        assertEquals(List.of(4, 1), libros.listarTarjetasPorIds(List.of(4, 2, 1, 3), FECHA)
                .stream().map(TarjetaLibroDTO::id).toList());
        assertEquals(List.of(4, 1), libros.listarTarjetasPorIds(List.of(4, 4, 1), FECHA)
                .stream().map(TarjetaLibroDTO::id).toList());
        assertArrayEquals(new byte[]{1, 2}, libros.listarTarjetasPorIds(List.of(1), FECHA)
                .getFirst().portada().contenido());
        List<TarjetaLibroDTO> sinImagen = libros.listarTarjetasSinImagenPorIds(List.of(4, 2, 1, 3), FECHA);
        assertEquals(List.of(4, 1), sinImagen.stream().map(TarjetaLibroDTO::id).toList());
        assertTrue(sinImagen.stream().allMatch(t -> t.portada() == null));
        assertEquals(libros.listarTarjetasPorIds(List.of(1), FECHA).getFirst().precioConDescuento(),
                libros.listarTarjetasSinImagenPorIds(List.of(1), FECHA).getFirst().precioConDescuento());
        assertTrue(libros.listarTarjetasPorIds(List.of(), FECHA).isEmpty());
    }

    @Test
    void detalleResumeTodasLasResenasYLosDatosDeCompraYGuardado() throws Exception {
        registrarVenta(primero, 1, "EN_CAMINO", 1, "100.01", FECHA);
        publicar(1, 2, 1, true, false);
        publicar(1, 2, 5, true, false);
        publicar(1, 2, 3, false, false);
        publicar(1, 2, 2, true, true);
        guardar(1, 1, true);
        guardar(1, 1, false);
        DetalleLibroDTO detalle = libros.obtenerDetalle(1, 1, FECHA);
        assertEquals(2, detalle.valoracion().cantidadResenas());
        assertEquals(3.0, detalle.valoracion().promedio());
        assertEquals(1, detalle.valoracion().cantidadUnaEstrella());
        assertEquals(1, detalle.valoracion().cantidadCincoEstrellas());
        assertEquals("Autor 1", detalle.libro().nombreAutor());
        assertEquals("Categoria 1", detalle.nombreCategoria());
        assertEquals("Editorial 1", detalle.nombreEditorial());
        assertEquals("Español", detalle.idioma());
        assertTrue(puedePublicarResena(1, 1));
        assertTrue(detalle.favorito());
        assertTrue(detalle.deseado());
        assertFalse(puedePublicarResena(1, 0));
        publicar(1, 1, 4, true, false);
        assertFalse(puedePublicarResena(1, 1));
        assertNull(libros.obtenerDetalle(2, 1, FECHA).valoracion().promedio());
    }

    @Test
    void canceladosDevueltosYUsuarioInactivoNoHabilitanResena() throws Exception {
        registrarVenta(primero, 1, "CANCELADO", 1, "100.01", FECHA);
        registrarVenta(primero, 1, "DEVUELTO", 1, "100.01", FECHA);
        assertFalse(puedePublicarResena(1, 1));
        registrarVenta(primero, 1, "EN_ALMACEN", 1, "100.01", FECHA);
        assertTrue(puedePublicarResena(1, 1));
        ejecutar("UPDATE USUARIO SET activo = FALSE WHERE id = 1");
        assertFalse(puedePublicarResena(1, 1));
    }

    @Test
    void gestionYEdicionSeparanDatosReferenciasPrecioStockEImagen() throws Exception {
        var gestion = libros.obtenerGestion(1);
        assertEquals("Proveedor", gestion.datos().proveedor());
        assertEquals(1, gestion.autor().id());
        assertNull(gestion.autor().imagen());
        Libro cambios = registrarDatos("Modificado", 2, "Francés", "999", 500, 500, 2025, 0);
        cambios.setId(1);
        cambios.setUsuarioActualizacion("admin");
        libros.modificar(cambios);
        Libro guardado = libros.leer(1);
        assertEquals("Modificado", guardado.getTitulo());
        assertEquals("Francés", guardado.getIdioma());
        assertEquals(1, guardado.getAutor().getId());
        assertEquals(1, guardado.getCategoria().getId());
        assertEquals(1, guardado.getEditorial().getId());
        assertNull(guardado.getAutor().getNombre());
        assertEquals(new BigDecimal("100.01"), guardado.getPrecioBase());
        assertEquals(10, guardado.getStockFisico());
        assertNotNull(guardado.getFechaActualizacion());
        libros.cambiarPrecio(1, new BigDecimal("25.00"), "admin");
        libros.cambiarStock(1, 0, "admin");
        assertEquals(new BigDecimal("25.00"), libros.leer(1).getPrecioBase());
        assertEquals(0, libros.leer(1).getStockFisico());
    }

    @Test
    void imagenesSeCambianYQuitanComoOperacionIndependiente() throws Exception {
        ImagenDTO imagen = new ImagenDTO(new byte[]{1, 2, 3}, "image/png");
        libros.cambiarImagen(1, imagen, "admin");
        autores.cambiarImagen(1, imagen, "admin");
        categorias.cambiarImagen(1, imagen, "admin");
        editoriales.cambiarImagen(1, imagen, "admin");
        assertArrayEquals(imagen.contenido(), libros.leer(1).getPortada());
        assertArrayEquals(imagen.contenido(), autores.leer(1).getImagen());
        assertArrayEquals(imagen.contenido(), categorias.leer(1).getImagen());
        assertArrayEquals(imagen.contenido(), editoriales.leer(1).getImagen());
        libros.cambiarImagen(1, null, "admin");
        autores.cambiarImagen(1, null, "admin");
        categorias.cambiarImagen(1, null, "admin");
        editoriales.cambiarImagen(1, null, "admin");
        assertNull(libros.leer(1).getPortada());
        assertNull(libros.leer(1).getTipoPortada());
        assertNull(autores.leer(1).getImagen());
        assertNull(categorias.leer(1).getImagen());
        assertNull(editoriales.leer(1).getImagen());
        assertEquals("Alfa", libros.leer(1).getTitulo());
    }

    @Test
    void vetosSeAcumulanSeRetiranSinNegativosYSePuedenRevertir() throws Exception {
        libros.cambiarVetoDirecto(1, true, "admin");
        assertFalse(libros.cambiarVetoDirecto(1, true, "admin"));
        assertTrue(autores.cambiarActivo(1, false, "admin"));
        assertFalse(autores.cambiarActivo(1, false, "admin"));
        categorias.cambiarActivo(1, false, "admin");
        editoriales.cambiarActivo(1, false, "admin");
        assertEquals(4, libros.leerParaActualizar(1).getNumeroVetos());
        assertEquals(1, libros.leer(1).getNumeroVetos());
        autores.cambiarActivo(1, true, "admin");
        assertEquals(3, libros.leerParaActualizar(1).getNumeroVetos());
        assertFalse(libros.leerParaActualizar(1).isActivo());
        categorias.cambiarActivo(1, true, "admin");
        editoriales.cambiarActivo(1, true, "admin");
        assertEquals(1, libros.leerParaActualizar(1).getNumeroVetos());
        TransactionContext.rollback();
        TransactionContext.close();
        assertTrue(autores.leer(1).isActivo());
        assertEquals(1, libros.leer(1).getNumeroVetos());
        libros.cambiarVetoDirecto(1, false, "admin");
        assertEquals(0, libros.leer(1).getNumeroVetos());
        assertTrue(libros.leer(1).isActivo());
    }

    @Test
    void eliminarReferenciasConservaSusLibrosYPropagaElVeto() throws Exception {
        assertTrue(autores.eliminar(1, "admin"));
        assertFalse(autores.eliminar(1, "admin"));
        assertFalse(autores.cambiarActivo(1, true, "admin"));
        TransactionContext.commit();
        TransactionContext.close();
        assertTrue(autores.leer(1).isEliminado());
        assertFalse(libros.leer(1).isEliminado());
        assertFalse(libros.leer(1).isActivo());
        assertEquals(1, libros.leer(1).getNumeroVetos());
        assertEquals(List.of(2), ids(libros.buscar(new FiltroLibroDTO(), FECHA)));
    }

    @Test
    void altaRechazaReferenciasQueDejaronDeEstarDisponibles() throws Exception {
        autores.cambiarActivo(1, false, "admin");
        categorias.cambiarActivo(1, false, "admin");
        Libro nuevo = registrarDatos("Nuevo", 1, "Español", "10", 100, 1, 2026, 0);
        assertThrows(SQLException.class, () -> libros.insertar(nuevo));
        assertEquals(0, nuevo.getId());
        TransactionContext.rollback();
        TransactionContext.close();
        assertEquals(3, libros.buscar(new FiltroLibroDTO(), FECHA).totalResultados());
    }

    @Test
    void stockSeDescuentaCondicionalmenteYComparteLaTransaccion() throws Exception {
        assertTrue(libros.descontarStock(1, 7, "ana@test"));
        assertFalse(libros.descontarStock(1, 4, "ana@test"));
        assertEquals(3, libros.leerParaActualizar(1).getStockFisico());
        assertEquals("ana@test", libros.leerParaActualizar(1).getUsuarioActualizacion());
        assertEquals(10, libros.leer(1).getStockFisico());
        TransactionContext.rollback();
        TransactionContext.close();
        assertEquals(10, libros.leer(1).getStockFisico());
        assertTrue(libros.reponerStock(1, 2, "admin"));
        assertEquals(12, libros.leerParaActualizar(1).getStockFisico());
        TransactionContext.commit();
        TransactionContext.close();
        libros.cambiarVetoDirecto(1, true, "admin");
        assertFalse(libros.descontarStock(1, 1, "ana@test"));
        assertThrows(IllegalArgumentException.class, () -> libros.descontarStock(1, -1, "admin"));
        assertThrows(IllegalArgumentException.class, () -> libros.reponerStock(1, 0, "admin"));
    }

    @Test
    void noCierraNiConfirmaLaConexionDelContexto() throws Exception {
        Connection compartida = TransactionContext.getConnection();
        libros.descontarStock(1, 3, "admin");
        autores.cambiarActivo(1, false, "admin");
        assertSame(compartida, TransactionContext.getConnection());
        assertFalse(compartida.isClosed());
        assertFalse(compartida.getAutoCommit());
        assertEquals(10, libros.leer(1).getStockFisico());
        assertTrue(autores.leer(1).isActivo());
        TransactionContext.commit();
        TransactionContext.close();
        assertEquals(7, libros.leer(1).getStockFisico());
        assertFalse(autores.leer(1).isActivo());
        assertFalse(libros.leer(1).isActivo());
    }

    @Test
    void referenciasBuscanOrdenanPaginarYUsanSeguimientoEnUnaTabla() throws Exception {
        var filtro = new FiltroReferenciaDTO("1", true, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 1);
        assertEquals(1, autores.buscar(filtro).totalResultados());
        assertEquals(1, categorias.buscar(filtro).totalResultados());
        assertEquals(1, editoriales.buscar(filtro).totalResultados());
        ejecutar("INSERT INTO SEGUIMIENTO (id_usuario,tipo_seguimiento,id_autor,usuario_creacion)"
                + " VALUES (1,'AUTOR',1,'ana@test')");
        ejecutar("INSERT INTO SEGUIMIENTO (id_usuario,tipo_seguimiento,id_categoria,usuario_creacion)"
                + " VALUES (1,'CATEGORIA',1,'ana@test')");
        ejecutar("INSERT INTO SEGUIMIENTO (id_usuario,tipo_seguimiento,id_editorial,usuario_creacion)"
                + " VALUES (1,'EDITORIAL',1,'ana@test')");
        assertTrue(autores.obtenerDetalle(1, 1).seguido());
        assertTrue(categorias.obtenerDetalle(1, 1).seguido());
        assertTrue(editoriales.obtenerDetalle(1, 1).seguido());
        assertFalse(autores.obtenerDetalle(1, 2).seguido());
        autores.cambiarImagen(1, new ImagenDTO(new byte[]{1}, "image/png"), "admin");
        assertNotNull(autores.buscar(filtro).items().getFirst().imagen());
        assertNull(autores.buscarOpciones(filtro).items().getFirst().imagen());
        assertNull(categorias.buscarOpciones(filtro).items().getFirst().imagen());
        assertNull(editoriales.buscarOpciones(filtro).items().getFirst().imagen());
        var recientes = new FiltroReferenciaDTO(null, true, OrdenReferencia.MAS_RECIENTES, 2, 1);
        assertEquals(2, autores.buscar(recientes).items().getFirst().id());
        assertEquals(2, categorias.buscar(recientes).items().getFirst().id());
        assertEquals(2, editoriales.buscar(recientes).items().getFirst().id());
    }

    @Test
    void editarReferenciasConservaImagenActivoYAuditoriaDeCreacion() throws Exception {
        Autor autor = autores.leer(1);
        autor.setNombre("Nuevo autor");
        autor.setBiografia("Nueva biografia");
        autor.setUsuarioActualizacion("admin");
        autores.modificar(autor);
        Categoria categoria = categorias.leer(1);
        categoria.setNombre("Nueva categoria");
        categoria.setDescripcion("Nueva descripcion");
        categoria.setUsuarioActualizacion("admin");
        categorias.modificar(categoria);
        Editorial editorial = editoriales.leer(1);
        editorial.setNombre("Nueva editorial");
        editorial.setDescripcion("Nueva descripcion");
        editorial.setUsuarioActualizacion("admin");
        editoriales.modificar(editorial);
        assertEquals("Nueva biografia", autores.obtenerDetalle(1, 0).introduccion());
        assertEquals("Nueva descripcion", categorias.obtenerDetalle(1, 0).introduccion());
        assertEquals("Nueva descripcion", editoriales.obtenerDetalle(1, 0).introduccion());
        assertEquals(FECHA.minusDays(1), autores.leer(1).getFechaCreacion());
        assertEquals("admin", autores.leer(1).getUsuarioActualizacion());
    }

    @Test
    void descuentosMuestranTresEstadosYConservanLimitesInclusivos() throws Exception {
        Descuento vigente = promocion(primero, "25", FECHA.minusDays(1), FECHA.plusDays(1));
        registrarDescuento(vigente);
        registrarDescuento(promocion(primero, "10", FECHA.plusDays(2), FECHA.plusDays(3)));
        registrarDescuento(promocion(primero, "5", FECHA.minusDays(3), FECHA.minusDays(2)));
        var lista = descuentos.listarPorLibro(1, FECHA, 1, 2);
        assertEquals(3, lista.totalResultados());
        assertEquals(2, lista.items().size());
        var estados = descuentos.listarPorLibro(1, FECHA, 1, 12).items().stream()
                .map(item -> item.estado()).toList();
        assertTrue(estados.containsAll(List.of(EstadoDescuento.PROXIMO, EstadoDescuento.VIGENTE,
                EstadoDescuento.VENCIDO)));
        assertNotNull(descuentos.obtenerVigente(1, vigente.getFechaInicio()));
        assertNotNull(descuentos.obtenerVigente(1, vigente.getFechaFin()));
        assertNull(descuentos.obtenerVigente(1, vigente.getFechaFin().plusSeconds(1)));
        assertNull(descuentos.leer(vigente.getId()).getLibro().getTitulo());
    }

    @Test
    void solapamientoConsideraFuturosLimitesYExclusionDelPropio() throws Exception {
        Descuento descuento = promocion(primero, "25", FECHA, FECHA.plusDays(1));
        registrarDescuento(descuento);
        assertTrue(descuentos.haySuperposicion(1, 0, FECHA.plusDays(1), FECHA.plusDays(2)));
        assertFalse(descuentos.haySuperposicion(1, descuento.getId(), FECHA, FECHA.plusDays(1)));
        assertFalse(descuentos.haySuperposicion(2, 0, FECHA, FECHA.plusDays(1)));
        descuento.setPorcentaje(new BigDecimal("30"));
        descuento.setUsuarioActualizacion("admin");
        descuentos.modificar(descuento);
        assertEquals(new BigDecimal("30.00"), descuentos.leerParaActualizar(descuento.getId()).getPorcentaje());
        descuentos.eliminar(descuento.getId(), "admin");
        assertFalse(descuentos.haySuperposicion(1, 0, FECHA, FECHA.plusDays(1)));
        TransactionContext.commit();
        TransactionContext.close();
        assertEquals(0, descuentos.listarPorLibro(1, FECHA, 1, 12).totalResultados());
    }

    @Test
    void actualizarVigenciasEsExplicitoYNoReviveEliminados() throws Exception {
        Descuento proximo = promocion(primero, "25", FECHA.plusDays(1), FECHA.plusDays(2));
        registrarDescuento(proximo);
        descuentos.actualizarVigencias(FECHA, "sistema");
        assertFalse(descuentos.leer(proximo.getId()).isActivo());
        assertNull(descuentos.obtenerVigente(1, FECHA));
        descuentos.actualizarVigencias(FECHA.plusDays(1), "sistema");
        assertTrue(descuentos.leer(proximo.getId()).isActivo());
        assertEquals(new BigDecimal("75.01"), libros.obtenerDetalle(1, 0, FECHA.plusDays(1))
                .libro().precioConDescuento());
        descuentos.eliminar(proximo.getId(), "admin");
        TransactionContext.commit();
        TransactionContext.close();
        descuentos.actualizarVigencias(FECHA.plusDays(1), "sistema");
        assertFalse(descuentos.leer(proximo.getId()).isActivo());
    }

    @Test
    void reporteUsaImportesHistoricosPeriodoCompletoYNoPaginasWeb() throws Exception {
        LocalDate dia = FECHA.toLocalDate();
        registrarVenta(primero, 1, "EN_ALMACEN", 2, "40.00", dia.atStartOfDay());
        registrarVenta(segundo, 1, "ENTREGADO", 1, "70.00", dia.atTime(23, 59, 59));
        registrarVenta(tercero, 1, "ENTREGADO", 100, "90.00", dia.plusDays(1).atStartOfDay());
        registrarVenta(tercero, 1, "CANCELADO", 100, "90.00", FECHA);
        registrarVenta(tercero, 1, "DEVUELTO", 100, "90.00", FECHA);
        libros.cambiarPrecio(1, new BigDecimal("999.00"), "admin");
        libros.eliminar(1, "admin");
        var reporte = libros.listarVentasLibros(new ConsultaReporteLibrosDTO(dia, dia, null));
        assertEquals(2, reporte.size());
        assertEquals(1, reporte.getFirst().idLibro());
        assertEquals(2, reporte.getFirst().cantidadVendida());
        assertEquals(new BigDecimal("80.00"), reporte.getFirst().importeVentas());
        assertEquals("Autor 1", reporte.getFirst().autor());
        assertEquals("Categoria 1", reporte.getFirst().categoria());
        assertEquals("Editorial 1", reporte.getFirst().editorial());
        assertEquals(1, libros.listarVentasLibros(new ConsultaReporteLibrosDTO(dia, dia, 1)).size());
        assertTrue(libros.listarVentasLibros(new ConsultaReporteLibrosDTO(dia.minusDays(2),
                dia.minusDays(1), null)).isEmpty());
        assertEquals(1, autores.obtenerMasCompradas(List.of(1, 2), dia, dia).get(1).id());
        assertEquals(1, categorias.obtenerMasCompradas(List.of(1, 2), dia, dia).get(1).id());
        assertEquals(1, editoriales.obtenerMasCompradas(List.of(1, 2), dia, dia).get(1).id());
        assertFalse(autores.obtenerMasCompradas(List.of(1, 2), dia, dia).containsKey(2));
    }

    @Test
    void rechazaParametrosInvalidosAntesDeConsultar() {
        assertThrows(IllegalArgumentException.class,
                () -> libros.buscar(filtro(OrdenLibro.RECOMENDADOS_PRIMERO, true, 1, 12), FECHA));
        assertThrows(IllegalArgumentException.class,
                () -> libros.buscar(filtro(OrdenLibro.MAS_RECIENTES, true, 0, 12), FECHA));
        assertThrows(IllegalArgumentException.class, () -> libros.cambiarPrecio(1, null, "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> libros.cambiarPrecio(1, BigDecimal.ZERO, "admin"));
        assertThrows(IllegalArgumentException.class, () -> descuentos.haySuperposicion(1, 0, FECHA, FECHA));
        assertThrows(IllegalArgumentException.class,
                () -> libros.listarVentasLibros(new ConsultaReporteLibrosDTO(FECHA.toLocalDate(),
                        FECHA.toLocalDate(), 0)));
    }

    private FiltroLibroDTO filtro(OrdenLibro orden, boolean activos, int pagina, int tamanio) {
        return new FiltroLibroDTO(null, null, null, null, null, null, null,
                activos, false, orden, pagina, tamanio);
    }

    private FiltroLibroDTO conTitulo(String titulo) {
        return new FiltroLibroDTO(titulo, null, null, null, null, null, null,
                true, false, OrdenLibro.TITULO_ASCENDENTE, 1, 12);
    }

    private List<Integer> ids(ResultadoPaginadoDTO<TarjetaLibroDTO> resultado) {
        return resultado.items().stream().map(TarjetaLibroDTO::id).toList();
    }

    private Libro registrarDatos(String titulo, int referencia, String idioma, String precio,
            int paginas, int stock, int anio, int antiguedad) {
        Libro libro = new Libro();
        Autor autor = new Autor();
        autor.setId(referencia);
        Categoria categoria = new Categoria();
        categoria.setId(referencia);
        Editorial editorial = new Editorial();
        editorial.setId(referencia);
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setCategoria(categoria);
        libro.setEditorial(editorial);
        libro.setIdioma(idioma);
        libro.setPrecioBase(new BigDecimal(precio));
        libro.setStockFisico(stock);
        libro.setNumeroPaginas(paginas);
        libro.setAnioEdicion(anio);
        libro.setSinopsis("Sinopsis");
        libro.setProveedor("Proveedor");
        libro.setIsbn(titulo);
        libro.setUsuarioCreacion("admin");
        libro.setFechaCreacion(FECHA.minusDays(antiguedad));
        return libro;
    }

    private Libro registrarLibro(String titulo, int referencia, String idioma, String precio,
            int paginas, int stock, int anio, int antiguedad) throws Exception {
        Libro libro = registrarDatos(titulo, referencia, idioma, precio, paginas, stock, anio, antiguedad);
        libros.insertar(libro);
        TransactionContext.commit();
        TransactionContext.close();
        return libro;
    }

    private Descuento promocion(Libro libro, String porcentaje, LocalDateTime inicio, LocalDateTime fin) {
        Descuento descuento = new Descuento(0, libro, new BigDecimal(porcentaje), inicio, fin);
        descuento.setUsuarioCreacion("admin");
        descuento.setFechaCreacion(FECHA.minusDays(4));
        return descuento;
    }

    private void registrarVenta(Libro libro, int usuario, String estado, int cantidad,
            String precio, LocalDateTime fecha) throws Exception {
        String sql = "INSERT INTO PEDIDO (id_usuario,subtotal_productos,total,cantidad_total_libros,"
                + " id_direccion_envio,id_metodo_pago,identificador_transaccion,fecha_pago,estado,"
                + " codigo_seguimiento,fecha_entrega_estimada,usuario_creacion)"
                + " VALUES (?, ?, ?, ?, 1, 1, ?, ?, ?, ?, ?, 'ana@test')";
        String sqlItem = "INSERT INTO ITEM_PEDIDO (id_pedido,id_libro,cantidad,precio_base,precio_unitario,"
                + " porcentaje_descuento,sub_total,usuario_creacion) VALUES (?, ?, ?, ?, ?, ?, ?, 'ana@test')";
        BigDecimal unitario = new BigDecimal(precio);
        BigDecimal porcentaje = libro.getPrecioBase().subtract(unitario).multiply(BigDecimal.valueOf(100))
                .divide(libro.getPrecioBase(), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal total = unitario.multiply(BigDecimal.valueOf(cantidad));
        int id;
        try (var statement = conexion.prepareStatement(sql,
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, usuario);
            statement.setBigDecimal(2, total);
            statement.setBigDecimal(3, total);
            statement.setInt(4, cantidad);
            statement.setString(5, UUID.randomUUID().toString());
            statement.setTimestamp(6, Timestamp.valueOf(fecha));
            statement.setString(7, estado);
            statement.setString(8, UUID.randomUUID().toString());
            statement.setTimestamp(9, Timestamp.valueOf(fecha.plusDays(1)));
            statement.executeUpdate();
            try (var claves = statement.getGeneratedKeys()) {
                assertTrue(claves.next());
                id = claves.getInt(1);
            }
        }
        ejecutar(sqlItem, id, libro.getId(), cantidad, libro.getPrecioBase(), unitario, porcentaje, total);
    }

    private void publicar(int libro, int usuario, int puntos, boolean activo, boolean eliminado) throws Exception {
        String sql = "INSERT INTO RESENA (id_libro,id_usuario,puntuacion,comentario,activo,eliminado,"
                + " usuario_creacion) VALUES (?, ?, ?, 'Comentario', ?, ?, 'ana@test')";
        ejecutar(sql, libro, usuario, puntos, activo, eliminado);
    }

    private void guardar(int usuario, int libro, boolean favorito) throws Exception {
        String sql = "INSERT INTO LIBRO_GUARDADO (id_usuario,id_libro,favorito,usuario_creacion)"
                + " VALUES (?, ?, ?, 'ana@test')";
        ejecutar(sql, usuario, libro, favorito);
    }

    private void ejecutar(String sql, Object... valores) throws Exception {
        try (var statement = conexion.prepareStatement(sql)) {
            for (int i = 0; i < valores.length; i++) {
                Object valor = valores[i];
                if (valor instanceof Integer entero) statement.setInt(i + 1, entero);
                else if (valor instanceof Boolean bandera) statement.setBoolean(i + 1, bandera);
                else if (valor instanceof BigDecimal importe) statement.setBigDecimal(i + 1, importe);
                else if (valor instanceof LocalDateTime fecha) {
                    statement.setTimestamp(i + 1, Timestamp.valueOf(fecha));
                } else statement.setString(i + 1, (String) valor);
            }
            statement.executeUpdate();
        }
    }

    @Test
    void tarjetaConservaLibrosNoDisponiblesParaCorregirCarrito() throws Exception {
        libros.cambiarVetoDirecto(1, true, "admin");
        libros.eliminar(2, "admin");
        assertFalse(libros.obtenerTarjeta(1, FECHA).activo());
        assertFalse(libros.obtenerTarjeta(2, FECHA).activo());
        assertEquals("Beta", libros.obtenerTarjeta(2, FECHA).titulo());
        assertNull(libros.obtenerTarjeta(999, FECHA));
        assertTrue(libros.listarTarjetasPorIds(List.of(1, 2), FECHA).isEmpty());
    }

    @Test
    void isbnExcluyePropioPeroNoIgnoraEliminados() throws Exception {
        assertTrue(libros.existeIsbn("Alfa", 0));
        assertFalse(libros.existeIsbn("Alfa", 1));
        assertFalse(libros.existeIsbn("Desconocido", 0));
        libros.eliminar(1, "admin");
        assertTrue(libros.existeIsbn("Alfa", 0));
    }

    @Test
    void promocionProgramadaSeAplicaSinEscribirDuranteLaConsulta() throws Exception {
        Descuento proximo = promocion(primero, "50", FECHA.plusDays(1), FECHA.plusDays(2));
        registrarDescuento(proximo);
        ejecutar("UPDATE DESCUENTO SET activo = FALSE WHERE id = ?", proximo.getId());
        LocalDateTime inicio = proximo.getFechaInicio();
        assertNotNull(descuentos.obtenerVigente(1, inicio));
        assertTrue(descuentos.obtenerVigente(1, inicio).estaVigente(inicio));
        assertEquals(new BigDecimal("50.01"), libros.obtenerDetalle(1, 0, inicio)
                .libro().precioConDescuento());
        assertEquals(new BigDecimal("50.01"), libros.obtenerTarjeta(1, inicio).precioConDescuento());
        assertEquals(new BigDecimal("50.01"), libros.listarTarjetasPorIds(List.of(1), inicio)
                .getFirst().precioConDescuento());
        FiltroLibroDTO filtro = new FiltroLibroDTO(null, null, null, null, null,
                new BigDecimal("50.01"), new BigDecimal("50.01"), true, false,
                OrdenLibro.PRECIO_ASCENDENTE, 1, 12);
        assertEquals(List.of(1), ids(libros.buscar(filtro, inicio)));
        assertFalse(descuentos.leer(proximo.getId()).isActivo());
        assertEquals(new BigDecimal("100.01"), libros.obtenerTarjeta(1, inicio.minusSeconds(1))
                .precioConDescuento());
    }

    @Test
    void escrituraImpideSolapamientoSinDependerDeUnaComprobacionAnterior() throws Exception {
        registrarDescuento(promocion(primero, "25", FECHA, FECHA.plusDays(1)));
        Descuento solapado = promocion(primero, "10", FECHA.plusHours(1), FECHA.plusDays(2));
        assertThrows(SQLException.class, () -> descuentos.insertar(solapado));
        assertEquals(0, solapado.getId());
        TransactionContext.rollback();
        TransactionContext.close();
        assertEquals(1, descuentos.listarPorLibro(1, FECHA, 1, 12).totalResultados());
        Descuento otro = promocion(primero, "10", FECHA.plusDays(2), FECHA.plusDays(3));
        registrarDescuento(otro);
        otro.setFechaInicio(FECHA.plusHours(1));
        otro.setUsuarioActualizacion("admin");
        assertThrows(SQLException.class, () -> descuentos.modificar(otro));
        TransactionContext.rollback();
        TransactionContext.close();
        assertEquals(FECHA.plusDays(2), descuentos.leer(otro.getId()).getFechaInicio());
        assertThrows(IllegalArgumentException.class,
                () -> descuentos.insertar(promocion(primero, "101", FECHA, FECHA.plusDays(1))));
    }

    @Test
    void altaYDescuentosSeReviertenSinCerrarLaConexionCompartida() throws Exception {
        Libro nuevo = registrarDatos("Temporal", 1, "Español", "10", 100, 1, 2026, 0);
        libros.insertar(nuevo);
        Connection compartida = TransactionContext.getConnection();
        descuentos.insertar(promocion(nuevo, "25", FECHA, FECHA.plusDays(1)));
        assertNull(libros.leer(nuevo.getId()));
        assertNotNull(libros.leerParaActualizar(nuevo.getId()));
        assertFalse(compartida.isClosed());
        TransactionContext.rollback();
        TransactionContext.close();
        assertNull(libros.leer(nuevo.getId()));
        assertEquals(0, descuentos.listarPorLibro(nuevo.getId(), FECHA, 1, 12).totalResultados());
    }

    @Test
    void operacionesIndependientesNoAbrenUnContextoDeTransaccion() throws Exception {
        try (MockedStatic<TransactionContext> contexto = mockStatic(TransactionContext.class)) {
            libros.buscar(new FiltroLibroDTO(), FECHA);
            libros.obtenerDetalle(1, 1, FECHA);
            libros.obtenerGestion(1);
            libros.obtenerTarjeta(1, FECHA);
            libros.listarTarjetasPorIds(List.of(1), FECHA);
            libros.listarParaRecomendaciones();
            libros.buscarIdiomas(null, true, 1, 12);
            libros.existeIsbn("Alfa", 0);
            categorias.existeNombre("Categoria 1", 0);
            editoriales.existeNombre("Editorial 1", 0);
            libros.cambiarPrecio(1, new BigDecimal("20"), "admin");
            libros.cambiarStock(1, 5, "admin");
            libros.cambiarImagen(1, null, "admin");
            libros.cambiarVetoDirecto(1, true, "admin");
            Libro libro = libros.leer(1);
            libro.setUsuarioActualizacion("admin");
            libros.modificar(libro);
            Autor autor = autores.leer(1);
            autor.setUsuarioActualizacion("admin");
            autores.modificar(autor);
            Categoria categoria = categorias.leer(1);
            categoria.setUsuarioActualizacion("admin");
            categorias.modificar(categoria);
            Editorial editorial = editoriales.leer(1);
            editorial.setUsuarioActualizacion("admin");
            editoriales.modificar(editorial);
            autores.buscar(new FiltroReferenciaDTO());
            categorias.buscarOpciones(new FiltroReferenciaDTO());
            editoriales.obtenerDetalle(1, 1);
            autores.cambiarImagen(1, null, "admin");
            categorias.cambiarImagen(1, null, "admin");
            editoriales.cambiarImagen(1, null, "admin");
            descuentos.leer(1);
            descuentos.obtenerVigente(1, FECHA);
            descuentos.listarPorLibro(1, FECHA, 1, 12);
            descuentos.actualizarVigencias(FECHA, "sistema");
            libros.listarVentasLibros(new ConsultaReporteLibrosDTO(FECHA.toLocalDate(),
                    FECHA.toLocalDate(), null));
            autores.obtenerMasCompradas(List.of(1), FECHA.toLocalDate(), FECHA.toLocalDate());
            categorias.obtenerMasCompradas(List.of(1), FECHA.toLocalDate(), FECHA.toLocalDate());
            editoriales.obtenerMasCompradas(List.of(1), FECHA.toLocalDate(), FECHA.toLocalDate());
            libros.eliminar(3, "admin");
            contexto.verifyNoInteractions();
        }
        for (Connection prestamo : prestamos) assertTrue(prestamo.isClosed());
    }

    @Test
    void errorSqlCierraElPrestamoPeroNoCierraUnaTransaccionCompartida() throws Exception {
        ejecutar("CREATE UNIQUE INDEX isbn_unico ON LIBRO(isbn)");
        Libro modificado = libros.leer(1);
        modificado.setIsbn("Beta");
        assertThrows(SQLException.class, () -> libros.modificar(modificado));
        assertTrue(prestamos.getLast().isClosed());
        Libro duplicado = registrarDatos("Beta", 1, "Español", "10", 100, 1, 2026, 0);
        assertThrows(SQLException.class, () -> libros.insertar(duplicado));
        assertFalse(TransactionContext.getConnection().isClosed());
        assertEquals("Alfa", libros.leer(1).getIsbn());
    }

    @Test
    void usaParametrosTipadosYNullsSqlSinAsignadorGenerico() throws Exception {
        List<PreparedStatement> consultas = new ArrayList<>();
        manager.reset();
        manager.when(DBManager::getConnection).thenAnswer(inv -> {
            Connection original = DriverManager.getConnection(url);
            Connection prestamo = spy(original);
            prestamos.add(prestamo);
            doAnswer(llamada -> {
                PreparedStatement consulta = spy(original.prepareStatement(
                        llamada.getArgument(0), java.sql.Statement.RETURN_GENERATED_KEYS));
                consultas.add(consulta);
                return consulta;
            }).when(prestamo).prepareStatement(anyString(), anyInt());
            doAnswer(llamada -> {
                PreparedStatement consulta = spy(original.prepareStatement(llamada.getArgument(0)));
                consultas.add(consulta);
                return consulta;
            }).when(prestamo).prepareStatement(anyString());
            return prestamo;
        });
        Libro nuevo = registrarDatos("Tipado", 1, "Español", "10", 100, 1, 2026, 0);
        libros.insertar(nuevo);
        TransactionContext.commit();
        TransactionContext.close();
        assertTrue(consultas.stream().anyMatch(consulta -> mockingDetails(consulta).getInvocations()
                .stream().anyMatch(inv -> inv.getMethod().getName().equals("setNull")
                        && inv.getArgument(0).equals(8) && inv.getArgument(1).equals(Types.DECIMAL))));
        assertTrue(consultas.stream().anyMatch(consulta -> mockingDetails(consulta).getInvocations()
                .stream().anyMatch(inv -> inv.getMethod().getName().equals("setNull")
                        && inv.getArgument(0).equals(12) && inv.getArgument(1).equals(Types.INTEGER))));
        libros.buscar(new FiltroLibroDTO(), FECHA);
        autores.buscar(new FiltroReferenciaDTO());
        for (PreparedStatement consulta : consultas) {
            assertTrue(consulta.isClosed());
            assertFalse(mockingDetails(consulta).getInvocations().stream()
                    .anyMatch(inv -> inv.getMethod().getName().equals("setObject")));
        }
    }

    // Business combina estas consultas antes de abrir el formulario; no es un campo del DTO.
    private boolean puedePublicarResena(int idLibro, int idUsuario) throws SQLException {
        var usuario = new me.DAO.usuario.UsuarioDAOImpl().leer(idUsuario);
        return usuario != null && usuario.isActivo() && !usuario.isEliminado()
                && new me.DAO.venta.PedidoDAOImpl().haCompradoLibro(idUsuario, idLibro)
                && !new ResenaDAOImpl().existePublicada(idUsuario, idLibro);
    }
    private void registrarDescuento(Descuento descuento) throws Exception {
        descuentos.insertar(descuento);
        TransactionContext.commit();
        TransactionContext.close();
    }

    @Test
    void nombresUnicosNoSeLiberanAlEliminarUnaReferencia() throws Exception {
        assertTrue(categorias.existeNombre("Categoria 1", 0));
        assertFalse(categorias.existeNombre("Categoria 1", 1));
        assertTrue(editoriales.existeNombre("Editorial 1", 0));
        assertFalse(editoriales.existeNombre("Editorial 1", 1));
        categorias.eliminar(1, "admin");
        editoriales.eliminar(1, "admin");
        TransactionContext.commit();
        TransactionContext.close();
        assertTrue(categorias.existeNombre("Categoria 1", 0));
        assertTrue(editoriales.existeNombre("Editorial 1", 0));
        assertFalse(categorias.existeNombre("Desconocida", 0));
        assertFalse(editoriales.existeNombre("Desconocida", 0));
    }

    @Test
    void reponerStockPermiteLibrosRetiradosYEvitaDesbordamiento() throws Exception {
        libros.eliminar(1, "admin");
        assertTrue(libros.reponerStock(1, 2, "admin"));
        assertEquals(12, libros.leerParaActualizar(1).getStockFisico());
        assertFalse(libros.descontarStock(1, 1, "admin"));
        assertFalse(libros.reponerStock(999, 1, "admin"));
        TransactionContext.commit();
        TransactionContext.close();
        libros.cambiarStock(2, Integer.MAX_VALUE, "admin");
        assertFalse(libros.reponerStock(2, 1, "admin"));
        assertEquals(Integer.MAX_VALUE, libros.leerParaActualizar(2).getStockFisico());
    }

    @Test
    void comprobacionDePeriodosBloqueaLibroAunqueNoTengaDescuentos() throws Exception {
        assertFalse(descuentos.haySuperposicion(1, 0, FECHA, FECHA.plusDays(1)));
        try (Connection otra = DriverManager.getConnection(url)) {
            otra.setAutoCommit(false);
            try (var configuracion = otra.createStatement()) {
                configuracion.execute("SET LOCK_TIMEOUT 50");
            }
            try (var consulta = otra.prepareStatement("SELECT id FROM LIBRO WHERE id = 1 FOR UPDATE")) {
                assertThrows(SQLException.class, consulta::executeQuery);
                TransactionContext.commit();
                TransactionContext.close();
                try (var rs = consulta.executeQuery()) {
                    assertTrue(rs.next());
                }
            }
            otra.rollback();
        }
    }

    @Test
    void editarDescuentoMantieneLibroOriginalYActualizaAuditoria() throws Exception {
        Descuento descuento = promocion(primero, "25", FECHA, FECHA.plusDays(1));
        registrarDescuento(descuento);
        Libro otraReferencia = new Libro();
        otraReferencia.setId(2);
        descuento.setLibro(otraReferencia);
        descuento.setPorcentaje(new BigDecimal("100"));
        descuento.setUsuarioActualizacion("admin");
        descuentos.modificar(descuento);
        Descuento guardado = descuentos.leerParaActualizar(descuento.getId());
        assertEquals(1, guardado.getLibro().getId());
        assertEquals("admin", guardado.getUsuarioActualizacion());
        assertNotNull(guardado.getFechaActualizacion());
        assertEquals(FECHA.minusDays(4), guardado.getFechaCreacion());
        TransactionContext.commit();
        TransactionContext.close();
        assertEquals(new BigDecimal("0.00"), libros.obtenerTarjeta(1, FECHA).precioConDescuento());
        assertNull(descuentos.obtenerVigente(2, FECHA));
    }

    @Test
    void referenciasDistinguenVetoDeEliminacionEnListasYDetalles() throws Exception {
        autores.cambiarActivo(1, false, "admin");
        categorias.cambiarActivo(1, false, "admin");
        editoriales.cambiarActivo(1, false, "admin");
        TransactionContext.commit();
        TransactionContext.close();
        var activos = new FiltroReferenciaDTO();
        var todos = new FiltroReferenciaDTO(null, false, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12);
        assertEquals(1, autores.buscar(activos).totalResultados());
        assertEquals(1, categorias.buscar(activos).totalResultados());
        assertEquals(1, editoriales.buscar(activos).totalResultados());
        assertEquals(2, autores.buscar(todos).totalResultados());
        assertEquals(2, categorias.buscar(todos).totalResultados());
        assertEquals(2, editoriales.buscar(todos).totalResultados());
        assertFalse(autores.obtenerDetalle(1, 0).activo());
        assertFalse(categorias.obtenerDetalle(1, 0).activo());
        assertFalse(editoriales.obtenerDetalle(1, 0).activo());
        autores.eliminar(1, "admin");
        categorias.eliminar(1, "admin");
        editoriales.eliminar(1, "admin");
        TransactionContext.commit();
        TransactionContext.close();
        assertNull(autores.obtenerDetalle(1, 0));
        assertNull(categorias.obtenerDetalle(1, 0));
        assertNull(editoriales.obtenerDetalle(1, 0));
        assertEquals(1, autores.buscar(todos).totalResultados());
        assertEquals(1, categorias.buscar(todos).totalResultados());
        assertEquals(1, editoriales.buscar(todos).totalResultados());
    }

    @Test
    void selectoresDeReferenciasTratanComodinesComoTexto() throws Exception {
        Autor autor = autores.leer(1);
        autor.setNombre("Autor 10%_!");
        autor.setUsuarioActualizacion("admin");
        autores.modificar(autor);
        Categoria categoria = categorias.leer(1);
        categoria.setNombre("Categoria 10%_!");
        categoria.setUsuarioActualizacion("admin");
        categorias.modificar(categoria);
        Editorial editorial = editoriales.leer(1);
        editorial.setNombre("Editorial 10%_!");
        editorial.setUsuarioActualizacion("admin");
        editoriales.modificar(editorial);
        var literal = new FiltroReferenciaDTO("%_!", true, OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12);
        assertEquals(1, autores.buscarOpciones(literal).totalResultados());
        assertEquals(1, categorias.buscarOpciones(literal).totalResultados());
        assertEquals(1, editoriales.buscarOpciones(literal).totalResultados());
        var ajeno = new FiltroReferenciaDTO("' OR 1=1 --", true, OrdenReferencia.MAS_RECIENTES, 1, 12);
        assertTrue(autores.buscarOpciones(ajeno).items().isEmpty());
        assertTrue(categorias.buscarOpciones(ajeno).items().isEmpty());
        assertTrue(editoriales.buscarOpciones(ajeno).items().isEmpty());
    }
}

