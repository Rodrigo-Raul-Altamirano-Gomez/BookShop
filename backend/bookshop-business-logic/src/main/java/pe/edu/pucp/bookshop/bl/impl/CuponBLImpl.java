package pe.edu.pucp.bookshop.bl.impl;

import pe.edu.pucp.bookshop.bl.CuponBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.ConfiguracionDAO;
import pe.edu.pucp.bookshop.dao.CuponDAO;
import pe.edu.pucp.bookshop.dao.PedidoDAO;
import pe.edu.pucp.bookshop.dao.SolicitudDevolucionDAO;
import pe.edu.pucp.bookshop.dao.impl.ConfiguracionDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.CuponDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.PedidoDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.SolicitudDevolucionDAOImpl;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.Configuracion;
import pe.edu.pucp.bookshop.model.Cupon;
import pe.edu.pucp.bookshop.model.EstadoPedido;
import pe.edu.pucp.bookshop.model.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.Pedido;
import pe.edu.pucp.bookshop.model.SolicitudDevolucion;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CuponBLImpl implements CuponBL {

    private CuponDAO cuponDAO = new CuponDAOImpl();
    private PedidoDAO pedidoDAO = new PedidoDAOImpl();
    private SolicitudDevolucionDAO solicitudDAO = new SolicitudDevolucionDAOImpl();
    private ConfiguracionDAO configuracionDAO = new ConfiguracionDAOImpl();

    // RF02.12 y UX-UI §9.5: primero los utilizables (del que vence antes al que vence después), luego los demás
    @Override
    public List<Cupon> listarPorUsuario(int idUsuario) throws BusinessLogicException {
        try {
            LocalDate hoy = LocalDate.now();
            List<Cupon> utilizables = new ArrayList<>();
            List<Cupon> otros = new ArrayList<>();
            for (Cupon cupon : cuponDAO.listarPorUsuario(idUsuario)) {
                // ERS §2.2: utilizable si está activo y su fecha de vencimiento no ha pasado
                cupon.setUtilizable(cupon.getActivo() && !hoy.isAfter(cupon.getFechaVencimiento()));
                if (cupon.getUtilizable()) {
                    utilizables.add(cupon);
                } else {
                    otros.add(cupon);
                }
            }
            utilizables.addAll(otros);
            return utilizables;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF15.1: los que se pueden elegir al pagar; el primero es el que vence antes
    @Override
    public List<Cupon> listarUtilizables(int idUsuario) throws BusinessLogicException {
        List<Cupon> utilizables = new ArrayList<>();
        for (Cupon cupon : listarPorUsuario(idUsuario)) {
            if (cupon.getUtilizable()) {
                utilizables.add(cupon);
            }
        }
        return utilizables;
    }

    // RN10: un pedido cuenta para cupón cuando pasa el plazo de devolución desde su entrega, sin devolución
    // pendiente (una aceptada lo deja DEVUELTO) y con un total de al menos cuponMontoMinimo.
    // Cada vez que los pedidos contados de un cliente llegan a un múltiplo de cuponCadaPedidos, recibe un cupón.
    @Override
    public void emitirCupones() throws BusinessLogicException {
        try {
            Configuracion configuracion = configuracionDAO.obtener();
            LocalDateTime ahora = LocalDateTime.now();
            for (Pedido pedido : pedidoDAO.listarPorEstado(EstadoPedido.ENTREGADO)) {
                if (pedido.getContadoParaCupon()) continue;
                if (!ahora.isAfter(pedido.getFechaEntrega().plusDays(configuracion.getDiasDevolucion()))) continue;
                if (pedido.getTotal() < configuracion.getCuponMontoMinimo()) continue;
                boolean devolucionPendiente = false;
                for (SolicitudDevolucion solicitud : solicitudDAO.listarPorPedido(pedido.getId())) {
                    if (solicitud.getEstado() == EstadoSolicitud.PENDIENTE) {
                        devolucionPendiente = true;
                    }
                }
                if (devolucionPendiente) continue;

                pedidoDAO.marcarContadoParaCupon(pedido.getId());
                int contados = pedidoDAO.contarContadosParaCupon(pedido.getUsuario().getId());
                if (contados % configuracion.getCuponCadaPedidos() == 0) {
                    Cupon cupon = new Cupon();
                    cupon.setUsuario(pedido.getUsuario());
                    cupon.setCodigo("BK" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
                    cupon.setPorcentaje(configuracion.getCuponPorcentaje());
                    cupon.setFechaEmision(LocalDate.now());
                    cupon.setFechaVencimiento(LocalDate.now().plusDays(configuracion.getCuponDiasVigencia()));
                    cupon.setActivo(true);
                    cuponDAO.insertar(cupon);
                }
            }
            TransactionContext.commit();
        } catch (SQLException ex) {
            TransactionContext.rollback();
            throw new PersistenciaException(ex);
        } finally {
            TransactionContext.close();
        }
    }
}
