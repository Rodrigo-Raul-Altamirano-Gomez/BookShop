package pe.edu.pucp.bookshop.dbmanager;

import java.sql.Connection;
import java.sql.SQLException;

// Conexión compartida por las operaciones de una misma transacción (una por hilo de la petición)
public class TransactionContext {

    private static final ThreadLocal<Connection> conexionActual = new ThreadLocal<>();

    public static Connection getConnection() throws SQLException {
        Connection con = conexionActual.get();
        if (con == null) {
            con = DBManager.getInstance().getConnection();
            con.setAutoCommit(false);
            conexionActual.set(con);
        }
        return con;
    }

    public static void commit() throws SQLException {
        Connection con = conexionActual.get();
        if (con != null) {
            con.commit();
        }
    }

    public static void rollback() {
        Connection con = conexionActual.get();
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void close() {
        Connection con = conexionActual.get();
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            conexionActual.remove();
        }
    }
}
