package pe.edu.pucp.bookshop.bl.usuario.impl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.usuario.DireccionBL;
import pe.edu.pucp.bookshop.dao.usuario.DireccionDAO;
import pe.edu.pucp.bookshop.dao.usuario.impl.DireccionDAOImpl;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.usuario.Direccion;

import java.sql.SQLException;
import java.util.List;

public class DireccionBLImpl implements DireccionBL {

    private DireccionDAO direccionDAO = new DireccionDAOImpl();

    @Override
    public List<Direccion> listarPorUsuario(int idUsuario) throws BusinessLogicException {
        try {
            return direccionDAO.listarActivasPorUsuario(idUsuario);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Direccion insertar(Direccion direccion) throws BusinessLogicException {
        try {
            direccion.setPredeterminada(false);
            direccion.setActiva(true);
            direccion.setId(direccionDAO.insertar(direccion));
            return direccion;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void modificar(Direccion direccion) throws BusinessLogicException {
        try {
            direccionDAO.modificar(direccion);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF02.7: solo una dirección predeterminada a la vez
    @Override
    public void marcarPredeterminada(Direccion direccion) throws BusinessLogicException {
        try {
            direccionDAO.quitarPredeterminada(direccion.getUsuario().getId());
            direccionDAO.marcarPredeterminada(direccion.getId());
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
            direccionDAO.desactivar(id);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }
}
