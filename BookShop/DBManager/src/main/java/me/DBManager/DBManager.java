package me.DBManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class DBManager {
    private static final String CONFIGURACION = "db";
    private static HikariDataSource pool;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(DBManager::close, "BookShop-cierre-pool"));
    }

    private DBManager() {
    }

    public static Connection getConnection() throws SQLException {
        return obtenerPool().getConnection();
    }

    private static synchronized HikariDataSource obtenerPool() throws SQLException {
        if (pool != null) return pool;
        try {
            ResourceBundle config = ResourceBundle.getBundle(CONFIGURACION);
            String host = config.getString("db.host");
            String port = config.getString("db.port");
            String schema = config.getString("db.schema");
            String user = config.getString("db.user");
            String password = config.getString("db.password");
            String url = String.format("jdbc:mysql://%s:%s/%s", host, port, schema);
            HikariConfig configuracion = new HikariConfig();
            configuracion.setPoolName("BookShop");
            configuracion.setJdbcUrl(url);
            configuracion.setUsername(user);
            configuracion.setPassword(password);
            configuracion.setMaximumPoolSize(Integer.parseInt(config.getString("db.pool.maximumSize")));
            configuracion.setMinimumIdle(Integer.parseInt(config.getString("db.pool.minimumIdle")));
            configuracion.setConnectionTimeout(Long.parseLong(config.getString("db.pool.connectionTimeout")));
            configuracion.setValidationTimeout(Long.parseLong(config.getString("db.pool.validationTimeout")));
            configuracion.addDataSourceProperty("connectTimeout", config.getString("db.connectTimeout"));
            configuracion.addDataSourceProperty("socketTimeout", config.getString("db.socketTimeout"));
            // Fechas de negocio y CURRENT_TIMESTAMP usan Lima, también en la sesión MySQL.
            configuracion.addDataSourceProperty("connectionTimeZone", "-05:00");
            configuracion.addDataSourceProperty("forceConnectionTimeZoneToSession", "true");
            // Inicia sin una espera adicional de conexion; getConnection aplica su propio limite.
            configuracion.setInitializationFailTimeout(-1);
            pool = new HikariDataSource(configuracion);
            return pool;
        } catch (MissingResourceException e) {
            throw new SQLException("No se encontró db.properties o falta alguna clave de configuración.", e);
        } catch (IllegalArgumentException e) {
            throw new SQLException("La configuracion del pool de conexiones no es valida.", e);
        }
    }

    // Cierra el pool al terminar la aplicacion; no se llama al terminar cada consulta.
    public static synchronized void close() {
        if (pool == null) return;
        pool.close();
        pool = null;
    }
}

