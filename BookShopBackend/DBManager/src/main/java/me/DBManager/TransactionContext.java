package me.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

public final class TransactionContext {
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    private TransactionContext() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conexion = connectionHolder.get();
        if (conexion == null) {
            conexion = DBManager.getConnection();
            // Business siempre cierra el contexto en finally, también si falla el inicio.
            connectionHolder.set(conexion);
            conexion.setAutoCommit(false);
        }
        return conexion;
    }

    public static void commit() throws SQLException {
        Connection conexion = connectionHolder.get();
        if (conexion != null) conexion.commit();
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
        connectionHolder.remove();
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
