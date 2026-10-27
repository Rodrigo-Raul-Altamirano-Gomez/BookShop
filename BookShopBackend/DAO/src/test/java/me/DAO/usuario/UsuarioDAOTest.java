package me.DAO.usuario;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.catalogo.OrdenReferencia;
import me.Model.dto.usuario.TipoSeguimiento;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.compra.BilleteraDigital;
import me.Model.entities.compra.DireccionEnvio;
import me.Model.entities.compra.MetodoPago;
import me.Model.entities.compra.TarjetaBancaria;
import me.Model.entities.compra.TipoTarjeta;
import me.Model.entities.usuario.Seguimiento;
import me.Model.entities.usuario.SeguimientoAutor;
import me.Model.entities.usuario.SeguimientoCategoria;
import me.Model.entities.usuario.SeguimientoEditorial;
import me.Model.entities.usuario.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioDAOTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 7);
    private Connection conexion;
    private MockedStatic<DBManager> manager;

    @BeforeEach
    void prepararBaseAislada() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        conexion = DriverManager.getConnection(url);
        manager = mockStatic(DBManager.class);
        manager.when(DBManager::getConnection).thenAnswer(inv -> DriverManager.getConnection(url));
        String auditoria = ", usuario_creacion VARCHAR(254) NOT NULL, fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "usuario_actualizacion VARCHAR(254), fecha_actualizacion DATETIME, eliminado BOOLEAN DEFAULT FALSE";
        try (var statement = conexion.createStatement()) {
            statement.execute("CREATE TABLE USUARIO (id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(254) UNIQUE, "
                    + "contrasenia VARCHAR(255), nombre VARCHAR(250), imagen BLOB, tipo_imagen VARCHAR(30), "
                    + "fecha_nacimiento DATETIME, activo BOOLEAN DEFAULT TRUE, rol VARCHAR(40) DEFAULT 'CLIENTE', "
                    + "tiempo_total BIGINT DEFAULT 0, cantidad_pedidos_entregados INT DEFAULT 0" + auditoria + ")");
            statement.execute("CREATE TABLE DIRECCION_ENVIO (id INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, "
                    + "alias VARCHAR(100), calle VARCHAR(255), distrito VARCHAR(100), ciudad VARCHAR(100), "
                    + "referencia VARCHAR(255), activo BOOLEAN DEFAULT TRUE, predeterminada BOOLEAN DEFAULT FALSE"
                    + auditoria + ", marca INT GENERATED ALWAYS AS (CASE WHEN predeterminada THEN 1 ELSE NULL END), "
                    + "UNIQUE(id_usuario, marca))");
            statement.execute("CREATE TABLE METODO_PAGO (id INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, "
                    + "titular VARCHAR(150), proveedor VARCHAR(100), alias VARCHAR(100), activo BOOLEAN DEFAULT TRUE, "
                    + "tipo_metodo VARCHAR(40), numero_tarjeta_cifrado VARCHAR(512), numero_telefono_cifrado VARCHAR(512), "
                    + "tipo_tarjeta VARCHAR(40), fecha_vencimiento DATE, predeterminado BOOLEAN DEFAULT FALSE"
                    + auditoria + ", marca INT GENERATED ALWAYS AS (CASE WHEN predeterminado THEN 1 ELSE NULL END), "
                    + "UNIQUE(id_usuario, marca))");
            statement.execute("CREATE TABLE SEGUIMIENTO (id INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, "
                    + "tipo_seguimiento VARCHAR(40), id_autor INT, id_categoria INT, id_editorial INT" + auditoria
                    + ", UNIQUE(id_usuario, id_autor), UNIQUE(id_usuario, id_categoria), UNIQUE(id_usuario, id_editorial))");
            statement.execute("CREATE TABLE LIBRO_GUARDADO (id INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, "
                    + "id_libro INT, favorito BOOLEAN" + auditoria + ", UNIQUE(id_usuario, id_libro, favorito))");
            statement.execute("CREATE TABLE AUTOR (id INT PRIMARY KEY, nombre VARCHAR(100), activo BOOLEAN DEFAULT TRUE, "
                    + "eliminado BOOLEAN DEFAULT FALSE, imagen BLOB, tipo_imagen VARCHAR(30))");
            statement.execute("CREATE TABLE CATEGORIA (id INT PRIMARY KEY, nombre VARCHAR(100), activo BOOLEAN DEFAULT TRUE, "
                    + "eliminado BOOLEAN DEFAULT FALSE, imagen BLOB, tipo_imagen VARCHAR(30))");
            statement.execute("CREATE TABLE EDITORIAL (id INT PRIMARY KEY, nombre VARCHAR(100), activo BOOLEAN DEFAULT TRUE, "
                    + "eliminado BOOLEAN DEFAULT FALSE, imagen BLOB, tipo_imagen VARCHAR(30))");
            statement.execute("INSERT INTO USUARIO (id,email,contrasenia,nombre,fecha_nacimiento,usuario_creacion) "
                    + "VALUES (1,'ana@test','hash','Ana','2000-01-01','ana@test'), "
                    + "(2,'bea@test','hash','Bea','2000-01-01','bea@test')");
            statement.execute("INSERT INTO AUTOR (id,nombre) VALUES (1,'Autor A'),(2,'Autor B')");
            statement.execute("INSERT INTO CATEGORIA (id,nombre) VALUES (1,'Categoria')");
            statement.execute("INSERT INTO EDITORIAL (id,nombre) VALUES (1,'Editorial')");
            statement.execute("INSERT INTO DIRECCION_ENVIO (id,id_usuario,alias,calle,distrito,ciudad,usuario_creacion) "
                    + "VALUES (1,1,'Casa','Calle','Distrito','Lima','ana@test'), "
                    + "(2,2,'Casa','Calle','Distrito','Lima','bea@test')");
        }
    }

    @AfterEach
    void cerrarBase() throws Exception {
        TransactionContext.rollback();
        TransactionContext.close();
        manager.close();
        try (var statement = conexion.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
        conexion.close();
    }

    @Test
    void direccionAjenaNoQuitaLaPredeterminadaYEliminarLaDesmarca() throws Exception {
        DireccionEnvioDAOImpl dao = new DireccionEnvioDAOImpl();
        assertTrue(dao.establecerPredeterminado(1, 1, "ana@test"));
        assertFalse(dao.establecerPredeterminado(1, 2, "ana@test"));
        assertFalse(dao.establecerPredeterminado(1, 999, "ana@test"));
        assertTrue(dao.leerParaActualizar(1).isPredeterminada());
        TransactionContext.commit();
        TransactionContext.close();
        assertTrue(dao.eliminar(1, "ana@test"));
        assertFalse(dao.leer(1).isPredeterminada());
        assertEquals(0, dao.listarPorUsuario(1, 1, 12).totalResultados());
    }

    @Test
    void editarDireccionConservaPropietarioCreacionYMarcaExistente() throws Exception {
        DireccionEnvioDAOImpl dao = new DireccionEnvioDAOImpl();
        assertTrue(dao.establecerPredeterminado(1, 1, "ana@test"));
        TransactionContext.commit();
        TransactionContext.close();
        DireccionEnvio direccion = dao.leer(1);
        LocalDateTime fechaOriginal = direccion.getFechaCreacion();
        direccion.getUsuario().setId(2);
        direccion.setUsuarioCreacion("otro");
        direccion.setFechaCreacion(LocalDateTime.of(1990, 1, 1, 0, 0));
        direccion.setAlias("Casa editada");
        direccion.setPredeterminada(false);
        direccion.setUsuarioActualizacion("ana@test");
        dao.modificar(direccion);
        TransactionContext.commit();
        TransactionContext.close();
        DireccionEnvio leida = dao.leer(1);
        assertEquals(1, leida.getUsuario().getId());
        assertEquals(fechaOriginal, leida.getFechaCreacion());
        assertEquals("ana@test", leida.getUsuarioCreacion());
        assertTrue(leida.isPredeterminada());
        assertEquals("Casa editada", leida.getAlias());
    }

    @Test
    void rollbackDelAltaYEleccionDeDireccionConservaLaAnterior() throws Exception {
        DireccionEnvioDAOImpl dao = new DireccionEnvioDAOImpl();
        assertTrue(dao.establecerPredeterminado(1, 1, "ana@test"));
        TransactionContext.commit();
        TransactionContext.close();
        Usuario propietario = new Usuario();
        propietario.setId(1);
        DireccionEnvio nueva = new DireccionEnvio(0, propietario, "Nueva", "Calle", "Distrito", "Lima", null);
        nueva.setUsuarioCreacion("ana@test");
        nueva.setPredeterminada(true);
        dao.insertar(nueva);
        assertFalse(nueva.isPredeterminada());
        assertTrue(dao.establecerPredeterminado(1, nueva.getId(), "ana@test"));
        assertTrue(dao.leerParaActualizar(nueva.getId()).isPredeterminada());
        assertFalse(dao.leerParaActualizar(1).isPredeterminada());
        TransactionContext.rollback();
        TransactionContext.close();
        assertNull(dao.leer(nueva.getId()));
        assertTrue(dao.leer(1).isPredeterminada());
        assertEquals(1, dao.listarPorUsuario(1, 1, 12).totalResultados());
    }

    @Test
    void billeteraSeGuardaCifradaYElVencimientoNoEscribeAlConsultar() throws Exception {
        BilleteraDigital billetera = new BilleteraDigital();
        Usuario propietario = new Usuario();
        propietario.setId(1);
        billetera.setUsuario(propietario);
        billetera.setTitular("Ana");
        billetera.setProveedor("Proveedor libre");
        billetera.setAlias("Celular");
        billetera.setNumeroTelefonoCifrado("telefono cifrado");
        billetera.setFechaVencimiento(FECHA);
        billetera.setUsuarioCreacion("ana@test");
        MetodoPagoDAOImpl dao = new MetodoPagoDAOImpl();
        dao.insertar(billetera);
        assertEquals(1, dao.listarParaComprobarNumeros(false).size());
        assertTrue(dao.listarParaComprobarNumeros(true).isEmpty());
        assertTrue(dao.establecerPredeterminado(1, billetera.getId(), FECHA, "ana@test"));
        TransactionContext.commit();
        TransactionContext.close();
        MetodoPago leido = dao.leer(billetera.getId());
        assertInstanceOf(BilleteraDigital.class, leido);
        assertEquals("telefono cifrado", ((BilleteraDigital) leido).getNumeroTelefonoCifrado());
        assertTrue(leido.isActivo());
        assertTrue(dao.listarPorUsuario(1, 1, 12).items().getFirst().isPredeterminado());
        assertFalse(dao.establecerPredeterminado(1, billetera.getId(), FECHA.plusDays(1), "ana@test"));
        assertTrue(dao.leerParaActualizar(billetera.getId()).isPredeterminado());
        TransactionContext.commit();
        TransactionContext.close();
        dao.desactivarVencidos(FECHA.plusDays(1), "sistema");
        assertFalse(dao.leer(billetera.getId()).isActivo());
        assertFalse(dao.leer(billetera.getId()).isPredeterminado());
        assertEquals(1, dao.listarPorUsuario(1, 1, 12).totalResultados());
    }

    @Test
    void recuperarMetodoRechazaPropietarioOSubtipoAjenosYConservaSuCreacion() throws Exception {
        TarjetaBancaria tarjeta = new TarjetaBancaria();
        Usuario propietario = new Usuario();
        propietario.setId(1);
        tarjeta.setUsuario(propietario);
        tarjeta.setTitular("Ana");
        tarjeta.setProveedor("Banco");
        tarjeta.setNumeroTarjetaCifrado("cifrado original");
        tarjeta.setTipoTarjeta(TipoTarjeta.CREDITO);
        tarjeta.setFechaVencimiento(FECHA.plusYears(1));
        tarjeta.setUsuarioCreacion("ana@test");
        MetodoPagoDAOImpl dao = new MetodoPagoDAOImpl();
        dao.insertar(tarjeta);
        TransactionContext.commit();
        TransactionContext.close();
        LocalDateTime creacionOriginal = dao.leer(tarjeta.getId()).getFechaCreacion();
        assertTrue(dao.eliminar(tarjeta.getId(), "ana@test"));
        tarjeta.setUsuarioActualizacion("ana@test");
        propietario.setId(2);
        assertFalse(dao.recuperar(tarjeta));
        propietario.setId(1);
        BilleteraDigital subtipoAjeno = new BilleteraDigital();
        subtipoAjeno.setId(tarjeta.getId());
        subtipoAjeno.setUsuario(propietario);
        subtipoAjeno.setTitular("Ana");
        subtipoAjeno.setProveedor("Banco");
        subtipoAjeno.setNumeroTelefonoCifrado("cifrado");
        subtipoAjeno.setFechaVencimiento(FECHA.plusYears(1));
        subtipoAjeno.setUsuarioActualizacion("ana@test");
        assertFalse(dao.recuperar(subtipoAjeno));
        tarjeta.setUsuarioCreacion("otro");
        tarjeta.setNumeroTarjetaCifrado("nuevo cifrado");
        assertTrue(dao.recuperar(tarjeta));
        assertFalse(dao.recuperar(tarjeta));
        TransactionContext.commit();
        TransactionContext.close();
        TarjetaBancaria recuperada = (TarjetaBancaria) dao.leer(tarjeta.getId());
        assertEquals(creacionOriginal, recuperada.getFechaCreacion());
        assertEquals("ana@test", recuperada.getUsuarioCreacion());
        assertEquals("nuevo cifrado", recuperada.getNumeroTarjetaCifrado());
        assertFalse(recuperada.isEliminado());
        assertFalse(recuperada.isPredeterminado());
        assertThrows(SQLException.class, () -> dao.modificar(subtipoAjeno));
    }

    @Test
    void seguimientoUnificadoRecuperaCadaSubtipoYRepetirAltaNoCambiaFecha() throws Exception {
        SeguimientoDAOImpl dao = new SeguimientoDAOImpl();
        dao.guardar(1, TipoSeguimiento.AUTOR, 1, "ana@test");
        dao.guardar(1, TipoSeguimiento.CATEGORIA, 1, "ana@test");
        dao.guardar(1, TipoSeguimiento.EDITORIAL, 1, "ana@test");
        try (var statement = conexion.createStatement();
             var resultado = statement.executeQuery("SELECT id FROM SEGUIMIENTO ORDER BY id")) {
            resultado.next();
            int idAutor = resultado.getInt(1);
            Seguimiento primero = dao.leer(idAutor);
            assertInstanceOf(SeguimientoAutor.class, primero);
            assertNull(primero.getFechaActualizacion());
            dao.guardar(1, TipoSeguimiento.AUTOR, 1, "otro");
            assertNull(dao.leer(idAutor).getFechaActualizacion());
            assertTrue(dao.retirar(1, TipoSeguimiento.AUTOR, 1, "ana@test"));
            assertFalse(dao.retirar(1, TipoSeguimiento.AUTOR, 1, "ana@test"));
            dao.guardar(1, TipoSeguimiento.AUTOR, 1, "ana@test");
            assertEquals(primero.getFechaCreacion(), dao.leer(idAutor).getFechaCreacion());
            assertEquals("ana@test", dao.leer(idAutor).getUsuarioCreacion());
            assertNotNull(dao.leer(idAutor).getFechaActualizacion());
            resultado.next();
            assertInstanceOf(SeguimientoCategoria.class, dao.leer(resultado.getInt(1)));
            resultado.next();
            assertInstanceOf(SeguimientoEditorial.class, dao.leer(resultado.getInt(1)));
        }
        assertEquals(1, dao.listarPorUsuario(1, TipoSeguimiento.AUTOR,
                OrdenReferencia.NOMBRE_ASCENDENTE, 1, 12).totalResultados());
        assertEquals(1, dao.listarPorUsuario(1, TipoSeguimiento.CATEGORIA,
                OrdenReferencia.MAS_RECIENTES, 1, 12).totalResultados());
        assertEquals(1, dao.listarPorUsuario(1, TipoSeguimiento.EDITORIAL,
                OrdenReferencia.MAS_RECIENTES, 1, 12).totalResultados());
        assertEquals(0, dao.listarPorUsuario(2, TipoSeguimiento.AUTOR,
                OrdenReferencia.MAS_RECIENTES, 1, 12).totalResultados());
    }

    @Test
    void insertarSeguimientoLeeLaReferenciaPorIdYPaginaPorFechaSinDuplicar() throws Exception {
        Usuario propietario = new Usuario();
        propietario.setId(1);
        Autor autor = new Autor();
        autor.setId(2);
        SeguimientoAutor nuevo = new SeguimientoAutor(0, propietario, autor);
        nuevo.setFechaCreacion(LocalDateTime.of(2020, 1, 1, 0, 0));
        nuevo.setUsuarioCreacion("ana@test");
        SeguimientoDAOImpl dao = new SeguimientoDAOImpl();
        dao.insertar(nuevo);
        dao.guardar(1, TipoSeguimiento.AUTOR, 1, "ana@test");
        var pagina = dao.listarPorUsuario(1, TipoSeguimiento.AUTOR, OrdenReferencia.MAS_RECIENTES, 1, 1);
        assertEquals(2, pagina.totalResultados());
        assertEquals(1, pagina.items().getFirst().id());
        assertEquals(2, dao.listarPorUsuario(1, TipoSeguimiento.AUTOR,
                OrdenReferencia.MAS_RECIENTES, 2, 1).items().getFirst().id());
        SeguimientoAutor leido = (SeguimientoAutor) dao.leer(nuevo.getId());
        assertEquals(2, leido.getAutor().getId());
        assertNull(leido.getAutor().getNombre());
        assertTrue(dao.eliminar(nuevo.getId(), "ana@test"));
        assertFalse(dao.eliminar(nuevo.getId(), "ana@test"));
    }

    @Test
    void guardadosIndependientesConservanIdCreacionYAltaRepetida() throws Exception {
        LibroGuardadoDAOImpl dao = new LibroGuardadoDAOImpl();
        dao.guardar(1, 10, true, "ana@test");
        dao.guardar(1, 10, false, "ana@test");
        try (var statement = conexion.createStatement();
             var resultado = statement.executeQuery("SELECT id FROM LIBRO_GUARDADO WHERE favorito")) {
            resultado.next();
            int id = resultado.getInt(1);
            var favorito = dao.leer(id);
            dao.guardar(1, 10, true, "otro");
            assertNull(dao.leer(id).getFechaActualizacion());
            assertTrue(dao.retirar(1, 10, true, "ana@test"));
            dao.guardar(1, 10, true, "ana@test");
            assertEquals(favorito.getFechaCreacion(), dao.leer(id).getFechaCreacion());
            assertEquals("ana@test", dao.leer(id).getUsuarioCreacion());
            assertTrue(dao.leer(id).isFavorito());
            assertFalse(dao.leer(id).isEliminado());
        }
        try (var statement = conexion.createStatement();
             var resultado = statement.executeQuery("SELECT COUNT(*) FROM LIBRO_GUARDADO WHERE NOT eliminado")) {
            resultado.next();
            assertEquals(2, resultado.getInt(1));
        }
    }

    @Test
    void perfilNoExponeHashYReactivacionConservaLaIdentidad() throws Exception {
        UsuarioDAOImpl dao = new UsuarioDAOImpl();
        dao.cambiarImagen(1, new ImagenDTO(new byte[]{1, 2}, "image/png"), "ana@test");
        assertEquals("ana@test", dao.obtenerPerfil(1).usuario().email());
        assertArrayEquals(new byte[]{1, 2}, dao.obtenerPerfil(1).usuario().imagen().contenido());
        assertTrue(dao.cambiarContrasenia(1, "nuevo hash", "ana@test"));
        assertEquals("nuevo hash", dao.obtenerPorEmail("ana@test").getContrasenia());
        dao.actualizarTiempoTotal(1, Duration.ofSeconds(75), "ana@test");
        dao.actualizarTiempoTotal(1, Duration.ofSeconds(15), "ana@test");
        assertEquals(Duration.ofSeconds(90), dao.leer(1).getTiempoTotal());
        assertThrows(IllegalArgumentException.class,
                () -> dao.actualizarTiempoTotal(1, Duration.ofSeconds(-1), "ana@test"));
        assertTrue(dao.cambiarActivo(1, false, "ana@test"));
        assertFalse(dao.cambiarActivo(1, false, "ana@test"));
        assertFalse(dao.cambiarContrasenia(1, "rechazado", "ana@test"));
        assertFalse(dao.cambiarImagen(1, null, "ana@test"));
        assertEquals(1, dao.obtenerPorEmail("ana@test").getId());
        assertTrue(dao.cambiarActivo(1, true, "ana@test"));
        assertEquals(Duration.ofSeconds(90), dao.leer(1).getTiempoTotal());
        assertEquals("nuevo hash", dao.leer(1).getContrasenia());
    }
}
