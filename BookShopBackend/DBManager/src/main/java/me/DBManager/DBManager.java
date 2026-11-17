package me.DBManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class DBManager {

    private static String url;
    private static String usuario;
    private static String contrasenia;

    // Se ejecuta una sola vez, cuando se usa DBManager por primera vez.
    static {
        ResourceBundle config = ResourceBundle.getBundle("db");
        String host = config.getString("db.host");
        String puerto = config.getString("db.port");
        String esquema = config.getString("db.schema");
        usuario = config.getString("db.user");
        contrasenia = config.getString("db.password");
        // La sesión MySQL usa la hora de Lima para las fechas del negocio.
        url = "jdbc:mysql://" + host + ":" + puerto + "/" + esquema
            + "?connectionTimeZone=-05:00&forceConnectionTimeZoneToSession=true";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, contrasenia);
    }
}
