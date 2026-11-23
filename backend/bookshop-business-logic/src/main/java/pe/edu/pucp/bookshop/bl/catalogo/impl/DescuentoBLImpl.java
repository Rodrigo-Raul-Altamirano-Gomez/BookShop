package pe.edu.pucp.bookshop.bl.catalogo.impl;

import pe.edu.pucp.bookshop.bl.catalogo.DescuentoBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.bl.exception.RegistroDuplicadoException;
import pe.edu.pucp.bookshop.dao.catalogo.DescuentoDAO;
import pe.edu.pucp.bookshop.dao.catalogo.impl.DescuentoDAOImpl;
import pe.edu.pucp.bookshop.model.catalogo.Descuento;
import pe.edu.pucp.bookshop.model.catalogo.EstadoDescuento;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DescuentoBLImpl implements DescuentoBL {

    private DescuentoDAO descuentoDAO = new DescuentoDAOImpl();

    // Primero los vigentes y programados; luego los finalizados, del más reciente al más antiguo (UX-UI §10.3)
    @Override
    public List<Descuento> listarPorLibro(int idLibro) throws BusinessLogicException {
        try {
            LocalDate hoy = LocalDate.now();
            List<Descuento> abiertos = new ArrayList<>();
            List<Descuento> finalizados = new ArrayList<>();
            for (Descuento descuento : descuentoDAO.listarPorLibro(idLibro)) {
                // ERS §2.3: vigente si hoy está entre su inicio y su fin
                if (hoy.isAfter(descuento.getFechaFin())) {
                    descuento.setEstado(EstadoDescuento.FINALIZADO);
                    finalizados.add(0, descuento);
                } else if (hoy.isBefore(descuento.getFechaInicio())) {
                    descuento.setEstado(EstadoDescuento.PROGRAMADO);
                    abiertos.add(descuento);
                } else {
                    descuento.setEstado(EstadoDescuento.VIGENTE);
                    abiertos.add(descuento);
                }
            }
            abiertos.addAll(finalizados);
            return abiertos;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Descuento insertar(Descuento descuento) throws BusinessLogicException {
        try {
            // RN05: los descuentos de un mismo libro no se superponen en fechas
            for (Descuento otro : descuentoDAO.listarPorLibro(descuento.getLibro().getId())) {
                if (!descuento.getFechaInicio().isAfter(otro.getFechaFin()) && !descuento.getFechaFin().isBefore(otro.getFechaInicio())) {
                    DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    throw new RegistroDuplicadoException("Se superpone con el descuento del "
                            + otro.getFechaInicio().format(formato) + " al " + otro.getFechaFin().format(formato) + ".");
                }
            }
            descuento.setId(descuentoDAO.insertar(descuento));
            return descuento;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void eliminar(int id) throws BusinessLogicException {
        try {
            descuentoDAO.eliminar(id);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }
}
