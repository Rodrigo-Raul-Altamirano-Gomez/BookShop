package me.Business.usuario;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.ValidacionException;
import me.Business.ProteccionDatos;
import me.DAO.usuario.DireccionEnvioDAO;
import me.DAO.usuario.DireccionEnvioDAOImpl;
import me.DAO.usuario.MetodoPagoDAOImpl;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.compra.DatosDireccionEnvioDTO;
import me.Model.dto.compra.DatosMetodoPagoDTO;
import me.Model.entities.compra.TarjetaBancaria;
import me.Model.entities.compra.TipoTarjeta;
import me.Model.entities.usuario.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

class DatosCompraBOTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-07T17:00:00Z"), ZoneId.of("America/Lima"));
    private static final LocalDate HOY = LocalDate.of(2026, 10, 7);
    @TempDir Path temporal;
    private String claveAnterior;
    private Connection conexion;
    private MockedStatic<DBManager> manager;
    private MetodoPagoDAOImpl metodos;
    private UsuarioDAOImpl usuarios;
    private MetodoPagoBOImpl bo;

    @BeforeEach
    void prepararBaseAislada() throws Exception {
        claveAnterior = System.getProperty("bookshop.clave.file");
        System.setProperty("bookshop.clave.file", temporal.resolve("clave.key").toString());
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
            statement.execute("INSERT INTO USUARIO (id,email,contrasenia,nombre,fecha_nacimiento,usuario_creacion) "
                    + "VALUES (1,'ana@test','hash','Ana','2000-01-01','ana@test'), "
                    + "(2,'bea@test','hash','Bea','2000-01-01','bea@test')");
        }
        metodos = new MetodoPagoDAOImpl();
        usuarios = new UsuarioDAOImpl();
        bo = new MetodoPagoBOImpl(metodos, usuarios, RELOJ);
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
        if (claveAnterior == null) System.clearProperty("bookshop.clave.file");
        else System.setProperty("bookshop.clave.file", claveAnterior);
    }

    private DatosMetodoPagoDTO tarjeta(String numero, boolean predeterminado) {
        return new DatosMetodoPagoDTO(true, "Ana", "Banco", "Tarjeta", numero, TipoTarjeta.CREDITO,
                HOY.plusYears(1), predeterminado);
    }

    private DatosMetodoPagoDTO billetera(String proveedor, String numero, boolean predeterminado) {
        return new DatosMetodoPagoDTO(false, "Ana", proveedor, "Celular", numero, null, null, predeterminado);
    }

    @Test
    void altaDeTarjetaNormalizaCifraDevuelveMascaraYRespetaVencimientoInclusive() throws Exception {
        var respuesta = bo.registrar(1, new DatosMetodoPagoDTO(true, " Ana ", " Banco ", " Tarjeta ",
                "1234-5678 9012.3456", TipoTarjeta.DEBITO, HOY, true));
        TarjetaBancaria guardada = (TarjetaBancaria) metodos.leer(respuesta.id());
        assertEquals("1234567890123456", ProteccionDatos.descifrar(guardada.getNumeroTarjetaCifrado()));
        assertNotEquals("1234567890123456", guardada.getNumeroTarjetaCifrado());
        assertEquals("**** 3456", respuesta.numeroEnmascarado());
        assertEquals("Ana", respuesta.titular());
        assertTrue(respuesta.vigente());
        assertTrue(respuesta.predeterminado());
        assertTrue(guardada.isPredeterminado());
        assertEquals("**** 3456", bo.listar(1, 1, 10).items().getFirst().numeroEnmascarado());
    }

    @Test
    void numerosDuplicadosSeComparanDescifradosGlobalmenteInclusoTrasEliminar() throws Exception {
        var propio = bo.registrar(1, tarjeta("1234567890123456", true));
        assertThrows(ConflictoException.class, () -> bo.registrar(2, tarjeta("1234 5678 9012 3456", true)));
        bo.eliminar(1, propio.id());
        assertThrows(ConflictoException.class, () -> bo.registrar(2, tarjeta("1234567890123456", false)));
        var original = metodos.leer(propio.id());
        var recuperado = bo.registrar(1, new DatosMetodoPagoDTO(true, "Nuevo titular", "Otro Banco", "Recuperada",
                "1234567890123456", TipoTarjeta.DEBITO, HOY.plusYears(2), true));
        assertEquals(propio.id(), recuperado.id());
        assertEquals(original.getFechaCreacion(), metodos.leer(propio.id()).getFechaCreacion());
        assertEquals(original.getUsuarioCreacion(), metodos.leer(propio.id()).getUsuarioCreacion());
        assertFalse(metodos.leer(propio.id()).isEliminado());
        assertEquals("Nuevo titular", recuperado.titular());
    }

    @Test
    void billeteraEsUnicaPorProveedorYNumeroNormalizadosYNoPorNumeroSolo() throws Exception {
        bo.registrar(1, billetera("Proveedor", "987 654 321", false));
        assertThrows(ConflictoException.class, () -> bo.registrar(2, billetera(" proveedor ", "987-654-321", false)));
        assertDoesNotThrow(() -> bo.registrar(2, billetera("Otro proveedor", "987654321", false)));
        assertThrows(ValidacionException.class, () -> bo.registrar(1, billetera("Proveedor", "887654321", false)));
        assertThrows(ValidacionException.class, () -> bo.registrar(1, billetera("Proveedor", "**** 4321", false)));
    }

    @Test
    void billeteraVigenteNoExtiendeFechaYEditarVencidaRenueva365ConNumeroConservado() throws Exception {
        var alta = bo.registrar(1, billetera("Proveedor", "987654321", true));
        assertEquals(HOY.plusDays(365), alta.fechaVencimiento());
        Clock treintaDias = Clock.fixed(RELOJ.instant().plus(Duration.ofDays(30)), RELOJ.getZone());
        var vigenteBO = new MetodoPagoBOImpl(metodos, usuarios, treintaDias);
        var editada = vigenteBO.modificar(1, alta.id(), billetera("Proveedor", " ", true));
        assertEquals(alta.fechaVencimiento(), editada.fechaVencimiento());
        Clock vencida = Clock.fixed(RELOJ.instant().plusSeconds(366L * 24 * 60 * 60), RELOJ.getZone());
        var renovacionBO = new MetodoPagoBOImpl(metodos, usuarios, vencida);
        var listaVencida = renovacionBO.listar(1, 1, 10).items().getFirst();
        assertFalse(listaVencida.vigente());
        assertFalse(listaVencida.predeterminado());
        var renovada = renovacionBO.modificar(1, alta.id(), billetera("Proveedor", null, false));
        assertEquals(alta.id(), renovada.id());
        assertEquals(LocalDate.now(vencida).plusDays(365), renovada.fechaVencimiento());
        assertEquals(alta.numeroEnmascarado(), renovada.numeroEnmascarado());
        assertTrue(renovada.vigente());
        assertFalse(renovada.predeterminado());
    }

    @Test
    void metodoAjenoYSubtipoDistintoNoPuedenEditarNiEliminar() throws Exception {
        var alta = bo.registrar(1, tarjeta("1234567890123456", false));
        assertThrows(AccesoDenegadoException.class, () -> bo.consultar(2, alta.id()));
        assertThrows(AccesoDenegadoException.class, () -> bo.eliminar(2, alta.id()));
        assertThrows(AccesoDenegadoException.class, () -> bo.modificar(2, alta.id(), tarjeta("1234567890123456", false)));
        assertThrows(ValidacionException.class, () -> bo.modificar(1, alta.id(), billetera("Proveedor", "987654321", false)));
        assertInstanceOf(TarjetaBancaria.class, metodos.leer(alta.id()));
        assertFalse(metodos.leer(alta.id()).isEliminado());
    }

    @Test
    void intentoPredeterminadoVencidoConservaAnteriorYDesmarcarNoEligeSustituto() throws Exception {
        var elegida = bo.registrar(1, tarjeta("1234567890123456", true));
        Usuario propietario = usuarios.leer(1);
        TarjetaBancaria vencida = new TarjetaBancaria(0, "Ana", "Banco", propietario, "Vencida",
                ProteccionDatos.cifrar("6543210987654321"), HOY.minusDays(1), TipoTarjeta.CREDITO);
        vencida.setUsuarioCreacion("ana@test");
        metodos.insertar(vencida);
        TransactionContext.commit();
        TransactionContext.close();
        assertThrows(ConflictoException.class, () -> bo.establecerPredeterminado(1, vencida.getId()));
        assertTrue(metodos.leer(elegida.id()).isPredeterminado());
        var desmarcada = bo.modificar(1, elegida.id(), tarjeta(null, false));
        assertFalse(desmarcada.predeterminado());
        assertFalse(metodos.leer(elegida.id()).isPredeterminado());
        assertFalse(metodos.leer(vencida.getId()).isPredeterminado());
    }

    @Test
    void fallarDespuesDeQuitarMetodoPredeterminadoRevierteCamposYMarca() throws Exception {
        var alta = bo.registrar(1, tarjeta("1234567890123456", true));
        MetodoPagoDAOImpl daoFallido = spy(metodos);
        doAnswer(inv -> {
            inv.callRealMethod();
            throw new SQLException("Fallo despues de retirar la marca");
        }).when(daoFallido).quitarPredeterminado(alta.id(), "ana@test");
        MetodoPagoBOImpl fallido = new MetodoPagoBOImpl(daoFallido, usuarios, RELOJ);
        assertThrows(PersistenciaException.class, () -> fallido.modificar(1, alta.id(),
                new DatosMetodoPagoDTO(true, "Titular modificado", "Banco", "Alias modificado", null,
                        TipoTarjeta.DEBITO, HOY.plusYears(1), false)));
        var conservado = bo.consultar(1, alta.id());
        assertEquals(alta.titular(), conservado.titular());
        assertEquals(alta.alias(), conservado.alias());
        assertEquals(alta.tipoTarjeta(), conservado.tipoTarjeta());
        assertTrue(conservado.predeterminado());
    }

    @Test
    void direccionEditadaConCheckboxFalseYErroresDeEleccionMantieneAtomicidad() throws Exception {
        DireccionEnvioDAO dao = new DireccionEnvioDAOImpl();
        DireccionEnvioBOImpl direcciones = new DireccionEnvioBOImpl(dao, usuarios, RELOJ);
        var primera = direcciones.registrar(1, new DatosDireccionEnvioDTO("Casa", "Calle", "Distrito", "Lima", null, true));
        var segunda = direcciones.registrar(1, new DatosDireccionEnvioDTO("Trabajo", "Otra", "Distrito", "Lima", null, false));
        assertThrows(AccesoDenegadoException.class, () -> direcciones.eliminar(2, primera.id()));
        DireccionEnvioDAO fallo = spy(dao);
        doReturn(false).when(fallo).establecerPredeterminado(1, segunda.id(), "ana@test");
        var boFallido = new DireccionEnvioBOImpl(fallo, usuarios, RELOJ);
        assertThrows(ConflictoException.class, () -> boFallido.modificar(1, segunda.id(),
                new DatosDireccionEnvioDTO("Cambio fallido", "Calle distinta", "Distrito", "Lima", null, true)));
        assertEquals("Trabajo", direcciones.consultar(1, segunda.id()).alias());
        assertTrue(direcciones.consultar(1, primera.id()).predeterminada());
        var quitada = direcciones.modificar(1, primera.id(),
                new DatosDireccionEnvioDTO("Casa editada", "Calle", "Distrito", "Lima", null, false));
        assertFalse(quitada.predeterminada());
        assertFalse(direcciones.consultar(1, segunda.id()).predeterminada());
    }
}
