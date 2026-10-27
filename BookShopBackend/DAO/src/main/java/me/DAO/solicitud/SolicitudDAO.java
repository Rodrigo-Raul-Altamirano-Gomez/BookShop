package me.DAO.solicitud;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.DAO.CrudDAO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.solicitud.*;
import me.Model.entities.solicitud.*;

// SOLICITUD guarda ambos subtipos; el filtro selecciona la pestaña correspondiente.
public interface SolicitudDAO extends CrudDAO<Solicitud> {
    Solicitud leerParaActualizar(int id) throws SQLException;
    ResultadoPaginadoDTO<ResumenSolicitudDTO> listar(int idUsuario, FiltroSolicitudDTO filtro) throws SQLException;
    DetalleDevolucionDTO obtenerDetalleDevolucion(int id) throws SQLException;
    DetalleReporteResenaDTO obtenerDetalleReporteResena(int id, boolean incluirContenidoActual) throws SQLException;
    boolean existeDevolucionPendiente(int idPedido) throws SQLException;
    boolean existeReportePendiente(int idUsuario, int idResena) throws SQLException;
    boolean resolver(int id, EstadoSolicitud estado, LocalDateTime fecha, String actor) throws SQLException;
}
