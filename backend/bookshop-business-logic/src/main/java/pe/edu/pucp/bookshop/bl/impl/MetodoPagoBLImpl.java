package pe.edu.pucp.bookshop.bl.impl;

import pe.edu.pucp.bookshop.bl.MetodoPagoBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.exception.RegistroDuplicadoException;
import pe.edu.pucp.bookshop.dao.MetodoPagoDAO;
import pe.edu.pucp.bookshop.dao.impl.MetodoPagoDAOImpl;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.MetodoPago;

import java.sql.SQLException;
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
