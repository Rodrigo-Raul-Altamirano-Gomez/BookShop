package me.DAO.usuario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import me.DAO.CrudDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.entities.compra.MetodoPago;

public interface MetodoPagoDAO extends CrudDAO<MetodoPago> {
    MetodoPago leerParaActualizar(int id) throws SQLException;
    // Estos datos cifrados son internos. Business crea los DTO con numeros enmascarados.
    ResultadoPaginadoDTO<MetodoPago> listarPorUsuario(int idUsuario, int pagina, int tamano) throws SQLException;
    List<MetodoPago> listarParaComprobarNumeros(boolean esTarjeta) throws SQLException;
    void desactivarVencidos(LocalDate fecha, String usuario) throws SQLException;
    boolean recuperar(MetodoPago metodo) throws SQLException;
    boolean establecerPredeterminado(int idUsuario, int idMetodo, LocalDate fecha, String usuario)
            throws SQLException;
    boolean quitarPredeterminado(int idMetodo, String usuario) throws SQLException;
}
