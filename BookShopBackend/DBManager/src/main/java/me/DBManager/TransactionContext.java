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
            try {
                conexion.setAutoCommit(false);
            } catch (SQLException e) {
                try {
                    conexion.close();
                } catch (SQLException cierre) {
                    e.addSuppressed(cierre);
                }
                throw e;
            }
            connectionHolder.set(conexion);
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
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connectionHolder.remove();
            }
        }
    }
}
