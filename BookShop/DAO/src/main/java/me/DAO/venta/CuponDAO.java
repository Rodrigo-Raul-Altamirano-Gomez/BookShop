package me.DAO.venta;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.CuponDTO;
import me.Model.entities.compra.Cupon;

public interface CuponDAO {
    void insertar(Cupon cupon) throws SQLException;
    Cupon leer(int id) throws SQLException;
    Cupon leerParaActualizar(int id) throws SQLException;
    ResultadoPaginadoDTO<CuponDTO> listarPorUsuario(int idUsuario, LocalDateTime fecha, int pagina, int tamanio) throws SQLException;
    void actualizarVigencias(LocalDateTime fecha, String actor) throws SQLException;
    boolean consumir(int id, int idUsuario, LocalDateTime fecha, String actor) throws SQLException;
    boolean liberar(int id, LocalDateTime fecha, String actor) throws SQLException;
}
