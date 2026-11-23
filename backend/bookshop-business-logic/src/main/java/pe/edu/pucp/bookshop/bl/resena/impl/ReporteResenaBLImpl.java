package pe.edu.pucp.bookshop.bl.resena.impl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.OperacionNoPermitidaException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.resena.ReporteResenaBL;
import pe.edu.pucp.bookshop.dao.resena.ReporteResenaDAO;
import pe.edu.pucp.bookshop.dao.resena.ResenaDAO;
import pe.edu.pucp.bookshop.dao.resena.impl.ReporteResenaDAOImpl;
import pe.edu.pucp.bookshop.dao.resena.impl.ResenaDAOImpl;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.comun.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.comun.ResultadoPagina;
import pe.edu.pucp.bookshop.model.resena.ReporteResena;
import pe.edu.pucp.bookshop.model.resena.Resena;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReporteResenaBLImpl implements ReporteResenaBL {

    private ReporteResenaDAO reporteDAO = new ReporteResenaDAOImpl();
    private ResenaDAO resenaDAO = new ResenaDAOImpl();

    // RF10.6–8
    @Override
    public ReporteResena insertar(ReporteResena reporte) throws BusinessLogicException {
        try {
            // RN14: un solo reporte pendiente del cliente sobre cada reseña
            if (reporteDAO.contarPorUsuarioYResena(reporte.getUsuario().getId(), reporte.getResena().getId(), EstadoSolicitud.PENDIENTE) > 0) {
                throw new OperacionNoPermitidaException("Ya reportaste esta reseña y está en revisión.");
            }
            // RF10.7: copia del texto y la puntuación de la reseña en este momento
            Resena resena = resenaDAO.obtener(reporte.getResena().getId());
            reporte.setContenidoReportado(resena.getComentario());
            reporte.setPuntuacionReportada(resena.getPuntuacion());
            reporte.setEstado(EstadoSolicitud.PENDIENTE);
            reporte.setFecha(LocalDateTime.now().withNano(0));
            reporte.setId(reporteDAO.insertar(reporte));
            return reporte;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public List<ReporteResena> listarPorUsuario(int idUsuario) throws BusinessLogicException {
        try {
            return reporteDAO.listarPorUsuario(idUsuario);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF03.10: el motivo y el comentario de un reporte pendiente
    @Override
    public void modificar(ReporteResena reporte) throws BusinessLogicException {
        try {
            reporteDAO.modificar(reporte);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void retirar(int id) throws BusinessLogicException {
        try {
            reporteDAO.cambiarEstado(id, EstadoSolicitud.RETIRADA);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }

    // RF04.8: los pendientes, del más antiguo al más reciente
    @Override
    public ResultadoPagina<ReporteResena> listarPendientes(int pagina, int tamano) throws BusinessLogicException {
        try {
            List<ReporteResena> pendientes = reporteDAO.listarPorEstado(EstadoSolicitud.PENDIENTE);
            int total = pendientes.size();
            int desde = Math.min((pagina - 1) * tamano, total);
            int hasta = Math.min(desde + tamano, total);
            return new ResultadoPagina<>(total, new ArrayList<>(pendientes.subList(desde, hasta)));
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF04.8: aceptar el reporte oculta la reseña, todo junto
    @Override
    public void aceptar(ReporteResena reporte) throws BusinessLogicException {
        try {
            reporteDAO.cambiarEstado(reporte.getId(), EstadoSolicitud.ACEPTADA);
            resenaDAO.cambiarActiva(reporte.getResena().getId(), false);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }

    // RF04.8: rechazar el reporte deja la reseña visible
    @Override
    public void rechazar(int id) throws BusinessLogicException {
        try {
            reporteDAO.cambiarEstado(id, EstadoSolicitud.RECHAZADA);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }
}
