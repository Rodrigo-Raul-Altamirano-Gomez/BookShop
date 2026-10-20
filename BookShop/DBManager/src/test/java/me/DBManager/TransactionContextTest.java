package me.DBManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransactionContextTest {
    private static final List<Driver> DRIVERS_ANTERIORES = new ArrayList<>();
    private static final DriverPrueba DRIVER = new DriverPrueba();

    @BeforeAll
    static void prepararDriver() throws SQLException {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            DRIVERS_ANTERIORES.add(driver);
            DriverManager.deregisterDriver(driver);
        }
        DriverManager.registerDriver(DRIVER);
    }

    @AfterAll
    static void restaurarDrivers() throws SQLException {
        DBManager.close();
        DriverManager.deregisterDriver(DRIVER);
        for (Driver driver : DRIVERS_ANTERIORES) DriverManager.registerDriver(driver);
    }

    @BeforeEach
    void reiniciarPool() {
        TransactionContext.close();
        DBManager.close();
        DRIVER.rechazarConexion = false;
    }

    @Test
    void reutilizaLaConexionHastaClose() throws SQLException {
        Connection primera;
        ConexionPrueba fisica;
        try {
            primera = TransactionContext.getConnection();
            fisica = primera.unwrap(ConexionPrueba.class);
            assertSame(primera, TransactionContext.getConnection());
            assertFalse(primera.getAutoCommit());
            TransactionContext.commit();
            assertFalse(primera.isClosed());
        } finally {
            TransactionContext.close();
        }
        assertEquals(1, fisica.commits);
        assertEquals(0, fisica.rollbacks);
        assertEquals(0, fisica.cierres);
        assertTrue(primera.isClosed());

        try {
            assertNotSame(primera, TransactionContext.getConnection());
        } finally {
            TransactionContext.close();
        }
    }

    @Test
    void haceRollbackCuandoSeSolicita() throws SQLException {
        ConexionPrueba fisica;
        try {
            fisica = TransactionContext.getConnection().unwrap(ConexionPrueba.class);
            TransactionContext.rollback();
        } finally {
            TransactionContext.close();
        }
        assertEquals(0, fisica.commits);
        assertEquals(1, fisica.rollbacks);
        assertEquals(0, fisica.cierres);
    }

    @Test
    void dbManagerEntregaConexionesIndependientes() throws SQLException {
        try (Connection primera = DBManager.getConnection();
             Connection segunda = DBManager.getConnection()) {
            assertNotSame(primera, segunda);
            assertNotSame(primera.unwrap(ConexionPrueba.class), segunda.unwrap(ConexionPrueba.class));
        }
    }

    @Test
    void configuraLaZonaDeLimaEnElDriverYLaSesion() throws SQLException {
        try (Connection conexion = DBManager.getConnection()) {
            assertEquals("-05:00", DRIVER.ultimaConfiguracion.getProperty("connectionTimeZone"));
            assertEquals("true", DRIVER.ultimaConfiguracion.getProperty("forceConnectionTimeZoneToSession"));
        }
    }

    @Test
    void cerrarDevuelveLaConexionFisicaParaReutilizarla() throws SQLException {
        ConexionPrueba fisica;
        try (Connection conexion = DBManager.getConnection()) {
            fisica = conexion.unwrap(ConexionPrueba.class);
        }
        assertFalse(fisica.cerrada);

        try (Connection conexion = DBManager.getConnection()) {
            assertSame(fisica, conexion.unwrap(ConexionPrueba.class));
        }
        DBManager.close();
        assertTrue(fisica.cerrada);
        assertEquals(1, fisica.cierres);
    }

    @Test
    void devolverConexionRevierteTrabajoPendienteYRestableceSuEstado() throws SQLException {
        ConexionPrueba fisica;
        try (Connection conexion = DBManager.getConnection()) {
            fisica = conexion.unwrap(ConexionPrueba.class);
            conexion.setAutoCommit(false);
            conexion.setReadOnly(true);
            conexion.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try (Statement statement = conexion.createStatement()) {
                statement.executeUpdate("UPDATE prueba SET valor = 1");
            }
        }

        assertEquals(1, fisica.rollbacks);
        try (Connection conexion = DBManager.getConnection()) {
            assertSame(fisica, conexion.unwrap(ConexionPrueba.class));
            assertTrue(conexion.getAutoCommit());
            assertFalse(conexion.isReadOnly());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, conexion.getTransactionIsolation());
        }
    }

    @Test
    @Timeout(5)
    void poolOcupadoFallaDentroDelTiempoConfigurado() throws SQLException {
        try (Connection primera = DBManager.getConnection();
             Connection segunda = DBManager.getConnection()) {
            assertThrows(SQLException.class, DBManager::getConnection);
            assertFalse(primera.isClosed());
            assertFalse(segunda.isClosed());
        }
    }

    @Test
    @Timeout(5)
    void servidorNoDisponibleNoBloqueaIndefinidamente() {
        DRIVER.rechazarConexion = true;
        assertThrows(SQLException.class, DBManager::getConnection);
    }

    @Test
    void cerrarPoolPermiteCrearUnoNuevoCuandoSeNecesita() throws SQLException {
        ConexionPrueba anterior;
        try (Connection conexion = DBManager.getConnection()) {
            anterior = conexion.unwrap(ConexionPrueba.class);
        }
        DBManager.close();
        try (Connection conexion = DBManager.getConnection()) {
            assertTrue(anterior.cerrada);
            assertNotSame(anterior, conexion.unwrap(ConexionPrueba.class));
        }
    }

    @Test
    void noComparteLaTransaccionConOtroHilo() throws SQLException, InterruptedException {
        try {
            Connection conexionPrincipal = TransactionContext.getConnection();
            AtomicReference<Connection> conexionOtroHilo = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread hilo = new Thread(() -> {
                try {
                    conexionOtroHilo.set(TransactionContext.getConnection());
                } catch (Throwable e) {
                    error.set(e);
                } finally {
                    TransactionContext.close();
                }
            });
            hilo.setDaemon(true);
            hilo.start();
            hilo.join(5000);
            assertFalse(hilo.isAlive());
            assertNull(error.get());
            assertNotSame(conexionPrincipal, conexionOtroHilo.get());
        } finally {
            TransactionContext.close();
        }
    }

    private static class DriverPrueba implements Driver {
        private volatile boolean rechazarConexion;
        private volatile Properties ultimaConfiguracion;

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) return null;
            if (rechazarConexion) throw new SQLException("Servidor de prueba no disponible.");
            ultimaConfiguracion = (Properties) info.clone();
            return new ConexionPrueba().proxy();
        }

        @Override
        public boolean acceptsURL(String url) {
            return url.startsWith("jdbc:mysql://unit-test:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() { return 1; }

        @Override
        public int getMinorVersion() { return 0; }

        @Override
        public boolean jdbcCompliant() { return false; }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }

    private static class ConexionPrueba {
        private int commits;
        private int rollbacks;
        private int cierres;
        private boolean cerrada;
        private boolean autoCommit = true;
        private boolean soloLectura;
        private int aislamiento = Connection.TRANSACTION_READ_COMMITTED;
        private int timeoutRed;

        private Connection proxy() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (objeto, metodo, argumentos) -> {
                        switch (metodo.getName()) {
                            case "setAutoCommit" -> {
                                autoCommit = (boolean) argumentos[0];
                                return null;
                            }
                            case "getAutoCommit" -> { return autoCommit; }
                            case "isValid" -> { return !cerrada; }
                            case "isReadOnly" -> { return soloLectura; }
                            case "setReadOnly" -> {
                                soloLectura = (boolean) argumentos[0];
                                return null;
                            }
                            case "getTransactionIsolation" -> { return aislamiento; }
                            case "setTransactionIsolation" -> {
                                aislamiento = (int) argumentos[0];
                                return null;
                            }
                            case "getNetworkTimeout" -> { return timeoutRed; }
                            case "setNetworkTimeout" -> {
                                timeoutRed = (int) argumentos[1];
                                return null;
                            }
                            case "clearWarnings", "abort" -> { return null; }
                            case "unwrap" -> { return ((Class<?>) argumentos[0]).cast(this); }
                            case "isWrapperFor" -> { return ((Class<?>) argumentos[0]).isInstance(this); }
                            case "createStatement" -> {
                                return Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                        new Class<?>[]{Statement.class}, (statement, accion, valores) -> {
                                            return switch (accion.getName()) {
                                                case "executeUpdate" -> 1;
                                                case "close" -> null;
                                                default -> throw new UnsupportedOperationException(
                                                        accion.getName());
                                            };
                                        });
                            }
                            case "commit" -> {
                                commits++;
                                return null;
                            }
                            case "rollback" -> {
                                rollbacks++;
                                return null;
                            }
                            case "close" -> {
                                cierres++;
                                cerrada = true;
                                return null;
                            }
                            case "isClosed" -> { return cerrada; }
                            case "toString" -> { return "ConexionPrueba"; }
                            default -> throw new UnsupportedOperationException(metodo.getName());
                        }
                    });
        }
    }
}
