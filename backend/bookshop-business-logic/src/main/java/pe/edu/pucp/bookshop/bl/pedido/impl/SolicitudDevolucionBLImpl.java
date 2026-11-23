package pe.edu.pucp.bookshop.bl.pedido.impl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.OperacionNoPermitidaException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.pedido.PedidoBL;
import pe.edu.pucp.bookshop.bl.pedido.SolicitudDevolucionBL;
import pe.edu.pucp.bookshop.dao.catalogo.LibroDAO;
import pe.edu.pucp.bookshop.dao.catalogo.impl.LibroDAOImpl;
import pe.edu.pucp.bookshop.dao.pedido.PedidoDAO;
import pe.edu.pucp.bookshop.dao.pedido.SolicitudDevolucionDAO;
import pe.edu.pucp.bookshop.dao.pedido.impl.PedidoDAOImpl;
import pe.edu.pucp.bookshop.dao.pedido.impl.SolicitudDevolucionDAOImpl;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.comun.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;
import pe.edu.pucp.bookshop.model.pedido.EstadoPedido;
import pe.edu.pucp.bookshop.model.pedido.PedidoDetalle;
import pe.edu.pucp.bookshop.model.pedido.SolicitudDevolucion;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SolicitudDevolucionBLImpl implements SolicitudDevolucionBL {

    private SolicitudDevolucionDAO solicitudDAO = new SolicitudDevolucionDAOImpl();
    private PedidoDAO pedidoDAO = new PedidoDAOImpl();
    private LibroDAO libroDAO = new LibroDAOImpl();
    private PedidoBL pedidoBL = new PedidoBLImpl();

    // RF03.6–7 y RN08
    @Override
    public SolicitudDevolucion insertar(SolicitudDevolucion solicitud) throws BusinessLogicException {
        try {
            PedidoDetalle detalle = pedidoBL.obtener(solicitud.getPedido().getId());
            if (detalle.pedido().getEstado() != EstadoPedido.ENTREGADO || !detalle.devolucionEnPlazo()) {
                throw new OperacionNoPermitidaException("El plazo para solicitar la devolución de este pedido terminó.");
            }
            if (detalle.devolucionPendiente()) {
                throw new OperacionNoPermitidaException("Este pedido ya tiene una solicitud de devolución en revisión.");
            }
            if (detalle.solicitudesRestantes() <= 0) {
                throw new OperacionNoPermitidaException("Alcanzaste el límite de solicitudes de devolución de este pedido.");
            }
            solicitud.setEstado(EstadoSolicitud.PENDIENTE);
            solicitud.setFecha(LocalDateTime.now().withNano(0));
            solicitud.setId(solicitudDAO.insertar(solicitud));
            return solicitud;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public List<SolicitudDevolucion> listarPorUsuario(int idUsuario) throws BusinessLogicException {
        try {
            return solicitudDAO.listarPorUsuario(idUsuario);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF03.10: el motivo y el comentario de una solicitud pendiente
    @Override
    public void modificar(SolicitudDevolucion solicitud) throws BusinessLogicException {
        try {
            solicitudDAO.modificar(solicitud);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF03.10: pasa a RETIRADA y sigue contando como intento (RF03.7)
    @Override
    public void retirar(int id) throws BusinessLogicException {
        try {
            solicitudDAO.cambiarEstado(id, EstadoSolicitud.RETIRADA);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }

    // RF04.7: las pendientes, de la más antigua a la más reciente
    @Override
    public ResultadoPagina<SolicitudDevolucion> listarPendientes(int pagina, int tamano) throws BusinessLogicException {
        try {
            List<SolicitudDevolucion> pendientes = solicitudDAO.listarPorEstado(EstadoSolicitud.PENDIENTE);
            int total = pendientes.size();
            int desde = Math.min((pagina - 1) * tamano, total);
            int hasta = Math.min(desde + tamano, total);
            return new ResultadoPagina<>(total, new ArrayList<>(pendientes.subList(desde, hasta)));
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF03.8 y RN09: el pedido pasa a DEVUELTO y se repone el stock, todo junto
    @Override
    public void aceptar(SolicitudDevolucion solicitud) throws BusinessLogicException {
        try {
            int idPedido = solicitud.getPedido().getId();
            solicitudDAO.cambiarEstado(solicitud.getId(), EstadoSolicitud.ACEPTADA);
            pedidoDAO.registrarDevolucion(idPedido, LocalDateTime.now().withNano(0));
            libroDAO.reponerStock(idPedido);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }

    // El pedido sigue ENTREGADO
    @Override
    public void rechazar(int id) throws BusinessLogicException {
        try {
            solicitudDAO.cambiarEstado(id, EstadoSolicitud.RECHAZADA);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }
}
