package pe.edu.pucp.bookshop.dbmanager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBManager {

    private static DBManager instance;
    // Pool de conexiones: abrir una conexión nueva contra la base de datos en AWS tarda cerca de un segundo,
    // así que se reutilizan. Al cerrar una conexión (try con recursos), vuelve al pool.
    private final HikariDataSource pool;

    private DBManager() {
        Properties properties = new Properties();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("db.properties");
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (ClassNotFoundException | IOException ex) {
            throw new RuntimeException(ex);
        }
        // En el despliegue los datos pueden llegar por variables de entorno
        String host = leer(properties, "host", "DB_HOST");
        String port = leer(properties, "port", "DB_PORT");
        String database = leer(properties, "database", "DB_NAME");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        // time_zone: NOW() y CURDATE() con la hora de Perú, aunque el servidor esté en UTC
        url += "?allowPublicKeyRetrieval=true&useSSL=false&sessionVariables=time_zone='-05:00'";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(leer(properties, "user", "DB_USER"));
        config.setPassword(leer(properties, "password", "DB_PASSWORD"));
        config.setMaximumPoolSize(10);
        this.pool = new HikariDataSource(config);
    }

    private String leer(Properties properties, String clave, String variableEntorno) {
        String valor = System.getenv(variableEntorno);
        if (valor == null) {
            valor = properties.getProperty(clave);
        }
        return valor;
    }

    public static synchronized DBManager getInstance() {
        if (instance == null)
            instance = new DBManager();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    // Al detener o redesplegar la aplicación se cierran sus conexiones; si no, quedan abiertas en la base de datos
    public void cerrar() {
        pool.close();
    }
}
