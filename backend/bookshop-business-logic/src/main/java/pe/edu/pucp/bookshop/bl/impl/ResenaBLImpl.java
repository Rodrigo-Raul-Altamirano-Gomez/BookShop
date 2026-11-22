package pe.edu.pucp.bookshop.bl.impl;

import pe.edu.pucp.bookshop.bl.ResenaBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.PedidoDAO;
import pe.edu.pucp.bookshop.dao.ReporteResenaDAO;
import pe.edu.pucp.bookshop.dao.ResenaDAO;
import pe.edu.pucp.bookshop.dao.impl.PedidoDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.ReporteResenaDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.ResenaDAOImpl;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.EstadoPedido;
import pe.edu.pucp.bookshop.model.Resena;
import pe.edu.pucp.bookshop.model.dto.ResenasLibro;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResenaBLImpl implements ResenaBL {

    private ResenaDAO resenaDAO = new ResenaDAOImpl();
    private ReporteResenaDAO reporteDAO = new ReporteResenaDAOImpl();
    private PedidoDAO pedidoDAO = new PedidoDAOImpl();

    // RF09.3 y RF10: la reseña propia primero; las ocultas solo para el administrador (RF10.9).
    // El resumen (promedio, cantidad y distribución de 5 a 1 estrellas) cuenta solo las visibles.
    @Override
    public ResenasLibro listarPorLibro(int idLibro, int idUsuario, boolean incluirOcultas) throws BusinessLogicException {
        try {
            List<Resena> resenas = new ArrayList<>();
            Resena propia = null;
            int[] porEstrellas = new int[5];
            int suma = 0;
            int cantidad = 0;
            for (Resena resena : resenaDAO.listarPorLibro(idLibro, idUsuario)) {
                if (resena.getActiva()) {
                    porEstrellas[5 - resena.getPuntuacion()]++;
                    suma += resena.getPuntuacion();
                    cantidad++;
                }
                if (resena.getUsuario().getId() == idUsuario) {
                    propia = resena;
                } else if (resena.getActiva() || incluirOcultas) {
                    resenas.add(resena);
                }
            }
            if (propia != null) {
                resenas.add(0, propia);
            }
            List<Integer> distribucion = new ArrayList<>();
            for (int estrellas : porEstrellas) {
                distribucion.add(estrellas);
            }
            double promedio = cantidad > 0 ? Math.round(suma * 10.0 / cantidad) / 10.0 : 0;
            // RF10.1–2: reseña quien tiene el libro en un pedido entregado y aún no lo reseñó
            boolean puedeResenar = idUsuario > 0 && propia == null
                    && pedidoDAO.contarCompras(idUsuario, idLibro, EstadoPedido.ENTREGADO) > 0;
            return new ResenasLibro(resenas, promedio, cantidad, distribucion, puedeResenar);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF10.3: Mis reseñas, de la más reciente a la más antigua
    @Override
    public List<Resena> listarPorUsuario(int idUsuario) throws BusinessLogicException {
        try {
            return resenaDAO.listarPorUsuario(idUsuario);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Resena insertar(Resena resena) throws BusinessLogicException {
        try {
            resena.setFecha(LocalDateTime.now().withNano(0));
            resena.setActiva(true);
            resena.setId(resenaDAO.insertar(resena));
            return resena;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void modificar(Resena resena) throws BusinessLogicException {
        try {
            resenaDAO.modificar(resena);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF10.2: al eliminar la reseña se van con ella sus votos y sus reportes
    @Override
    public void eliminar(int id) throws BusinessLogicException {
        try {
            reporteDAO.eliminarPorResena(id);
            resenaDAO.eliminarVotos(id);
            resenaDAO.eliminar(id);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }

    // RF10.5 y RN14: un voto por reseña; volver a pulsar lo retira
    @Override
    public void alternarVoto(int idResena, int idUsuario) throws BusinessLogicException {
        try {
            if (resenaDAO.existeVoto(idResena, idUsuario)) {
                resenaDAO.eliminarVoto(idResena, idUsuario);
            } else {
                resenaDAO.insertarVoto(idResena, idUsuario);
            }
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF04.9: el administrador oculta o vuelve a mostrar cualquier reseña
    @Override
    public void cambiarVisibilidad(int id, boolean activa) throws BusinessLogicException {
        try {
            resenaDAO.cambiarActiva(id, activa);
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }
}
