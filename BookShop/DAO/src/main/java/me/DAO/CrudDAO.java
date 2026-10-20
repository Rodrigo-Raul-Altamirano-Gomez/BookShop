package me.DAO;

import java.sql.SQLException;

public interface CrudDAO<T> {
    void insertar(T objeto) throws SQLException;
    void modificar(T objeto) throws SQLException;
    T leer(int id) throws SQLException;
    boolean eliminar(int id, String usuario) throws SQLException;
}
