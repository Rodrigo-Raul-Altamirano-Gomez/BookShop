package me.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

// Guarda una conexión por hilo para que varias operaciones DAO formen una sola transacción.
public class TransactionContext {

    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public static Connection getConnection() throws SQLException {
        Connection conexion = connectionHolder.get();
        if (conexion == null) {
            conexion = DBManager.getConnection();
            conexion.setAutoCommit(false);
            connectionHolder.set(conexion);
        }
        return conexion;
    }

    public static void commit() throws SQLException {
        Connection conexion = connectionHolder.get();
        if (conexion != null)
            conexion.commit();
    }

    public static void rollback() {
        Connection conexion = connectionHolder.get();
        if (conexion != null) {
            try {
                conexion.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close() {
        Connection conexion = connectionHolder.get();
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            connectionHolder.remove();
        }
    }
}
