package me.DAO.usuario;

import java.sql.SQLException;

public interface LibroGuardadoDAO {

    void guardar(int idUsuario, int idLibro, boolean favorito, String usuario) throws SQLException;

    boolean retirar(int idUsuario, int idLibro, boolean favorito, String usuario) throws SQLException;
}
