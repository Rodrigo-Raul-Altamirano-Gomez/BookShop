package me.DAO.catalogo;

import java.sql.SQLException;

public interface VotoUtilDAO {
    boolean cambiarVoto(int idUsuario, int idResena, boolean marcar, String actor) throws SQLException;
}
