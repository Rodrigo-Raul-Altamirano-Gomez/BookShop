package pe.edu.pucp.bookshop.bl.usuario.impl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.exception.RegistroDuplicadoException;
import pe.edu.pucp.bookshop.bl.usuario.MetodoPagoBL;
import pe.edu.pucp.bookshop.dao.usuario.MetodoPagoDAO;
import pe.edu.pucp.bookshop.dao.usuario.impl.MetodoPagoDAOImpl;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.usuario.MetodoPago;
import pe.edu.pucp.bookshop.model.usuario.TipoMetodoPago;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class MetodoPagoBLImpl implements MetodoPagoBL {

    private MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAOImpl();

    @Override
    public List<MetodoPago> listarPorUsuario(int idUsuario) throws BusinessLogicException {
        try {
            return metodoPagoDAO.listarActivosPorUsuario(idUsuario);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF15.1: al pagar se ofrecen los activos y no vencidos; una tarjeta vence al terminar su mes (MM/AA)
    @Override
    public List<MetodoPago> listarVigentesPorUsuario(int idUsuario) throws BusinessLogicException {
        try {
            YearMonth mesActual = YearMonth.now();
            List<MetodoPago> vigentes = new ArrayList<>();
            for (MetodoPago metodo : metodoPagoDAO.listarActivosPorUsuario(idUsuario)) {
                if (metodo.getTipo() == TipoMetodoPago.TARJETA) {
                    int mes = Integer.parseInt(metodo.getFechaVencimiento().substring(0, 2));
                    int anio = 2000 + Integer.parseInt(metodo.getFechaVencimiento().substring(3, 5));
                    if (YearMonth.of(anio, mes).isBefore(mesActual)) {
                        continue;
                    }
                }
                vigentes.add(metodo);
            }
            return vigentes;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public MetodoPago insertar(MetodoPago metodoPago) throws BusinessLogicException {
        try {
            // RN04: el cliente no repite el número de un método de pago
            if (metodoPagoDAO.contarActivosConNumero(metodoPago.getUsuario().getId(), metodoPago.getNumero(), 0) > 0) {
                throw new RegistroDuplicadoException("Ya registraste un método de pago con este número.");
            }
            metodoPago.setPredeterminado(false);
            metodoPago.setActivo(true);
            metodoPago.setId(metodoPagoDAO.insertar(metodoPago));
            return metodoPago;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void modificar(MetodoPago metodoPago) throws BusinessLogicException {
        try {
            // RN04
            if (metodoPagoDAO.contarActivosConNumero(metodoPago.getUsuario().getId(), metodoPago.getNumero(), metodoPago.getId()) > 0) {
                throw new RegistroDuplicadoException("Ya registraste un método de pago con este número.");
            }
            metodoPagoDAO.modificar(metodoPago);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF02.11: solo un método predeterminado a la vez
    @Override
    public void marcarPredeterminado(MetodoPago metodoPago) throws BusinessLogicException {
        try {
            metodoPagoDAO.quitarPredeterminado(metodoPago.getUsuario().getId());
            metodoPagoDAO.marcarPredeterminado(metodoPago.getId());
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }

    // RN03: no se borra, se desactiva
    @Override
    public void desactivar(int id) throws BusinessLogicException {
        try {
            metodoPagoDAO.desactivar(id);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }
}
