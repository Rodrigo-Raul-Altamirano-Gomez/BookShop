package me.Business.solicitud;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.solicitud.DatosDevolucionDTO;
import me.Model.dto.solicitud.DatosReporteResenaDTO;
import me.Model.dto.solicitud.DetalleDevolucionDTO;
import me.Model.dto.solicitud.DetalleReporteResenaDTO;
import me.Model.dto.solicitud.FiltroSolicitudDTO;
import me.Model.dto.solicitud.ResumenSolicitudDTO;
import me.Model.entities.solicitud.EstadoSolicitud;

public interface SolicitudBO {
    void comprobarDevolucion(int idActor, int idPedido) throws NegocioException;
    void comprobarReporte(int idActor, int idResena) throws NegocioException;
    DetalleDevolucionDTO enviarDevolucion(int idActor, int idPedido, DatosDevolucionDTO datos) throws NegocioException;
    DetalleReporteResenaDTO enviarReporte(int idActor, int idResena, DatosReporteResenaDTO datos) throws NegocioException;
    DetalleDevolucionDTO editarDevolucion(int idActor, int idSolicitud, DatosDevolucionDTO datos) throws NegocioException;
    DetalleReporteResenaDTO editarReporte(int idActor, int idSolicitud, DatosReporteResenaDTO datos) throws NegocioException;
    void retirar(int idActor, int idSolicitud) throws NegocioException;
    ResultadoPaginadoDTO<ResumenSolicitudDTO> listarPropias(int idActor, FiltroSolicitudDTO filtro) throws NegocioException;
    ResultadoPaginadoDTO<ResumenSolicitudDTO> listarModeracion(int idActor, FiltroSolicitudDTO filtro) throws NegocioException;
    DetalleDevolucionDTO consultarDevolucion(int idActor, int idSolicitud) throws NegocioException;
    DetalleReporteResenaDTO consultarReporte(int idActor, int idSolicitud) throws NegocioException;
    DetallePedidoDTO consultarPedidoReferencia(int idActor, int idSolicitud) throws NegocioException;
    DetalleDevolucionDTO resolverDevolucion(int idActor, int idSolicitud, EstadoSolicitud resultado) throws NegocioException;
    DetalleReporteResenaDTO resolverReporte(int idActor, int idSolicitud, EstadoSolicitud resultado) throws NegocioException;
}
