package me.DAO.venta;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.entities.compra.Cupon;

public interface CuponDAO {

    void insertar(Cupon cupon) throws SQLException;

    Cupon leer(int id) throws SQLException;

    ResultadoPaginadoDTO<Cupon> listarPorUsuario(int idUsuario, int pagina, int
        tamanioPagina) throws SQLException;

    boolean consumir(int id, int idUsuario, LocalDateTime fecha, String actor) throws SQLException;

    boolean liberar(int id, LocalDateTime fecha, String actor) throws SQLException;
}
