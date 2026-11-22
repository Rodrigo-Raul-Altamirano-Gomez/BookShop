package pe.edu.pucp.bookshop.bl.impl;

import pe.edu.pucp.bookshop.bl.SeguimientoBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.SeguimientoDAO;
import pe.edu.pucp.bookshop.dao.impl.SeguimientoDAOImpl;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.SeguimientoEntidad;
import pe.edu.pucp.bookshop.model.TipoEntidad;
import pe.edu.pucp.bookshop.model.Usuario;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

import java.sql.SQLException;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SeguimientoBLImpl implements SeguimientoBL {

    private SeguimientoDAO seguimientoDAO = new SeguimientoDAOImpl();
    // Orden alfabético en español (tildes y ñ)
    private Collator alfabetico = Collator.getInstance(Locale.of("es"));

    // RF05.4: búsqueda por nombre y orden por nombre o por fecha de seguimiento ("fecha": el más reciente primero).
    // Lo inactivo no se muestra (RF04.2).
    @Override
    public ResultadoPagina<Entidad> listar(int idUsuario, TipoEntidad tipo, String texto, String orden, int pagina, int tamano) throws BusinessLogicException {
        try {
            List<SeguimientoEntidad> seguimientos = new ArrayList<>();
            for (SeguimientoEntidad seguimiento : seguimientoDAO.listarPorUsuario(idUsuario, tipo)) {
                if (!seguimiento.getEntidad().getActivo()) continue;
                if (texto != null && !seguimiento.getEntidad().getNombre().toLowerCase().contains(texto.toLowerCase())) continue;
                seguimientos.add(seguimiento);
            }
            if ("fecha".equals(orden)) {
                seguimientos.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
            } else if ("nombre-desc".equals(orden)) {
                seguimientos.sort((a, b) -> alfabetico.compare(b.getEntidad().getNombre(), a.getEntidad().getNombre()));
            } else {
                seguimientos.sort((a, b) -> alfabetico.compare(a.getEntidad().getNombre(), b.getEntidad().getNombre()));
            }
            int total = seguimientos.size();
            int desde = Math.min((pagina - 1) * tamano, total);
            int hasta = Math.min(desde + tamano, total);
            List<Entidad> entidades = new ArrayList<>();
            for (SeguimientoEntidad seguimiento : seguimientos.subList(desde, hasta)) {
                entidades.add(seguimiento.getEntidad());
            }
            return new ResultadoPagina<>(total, entidades);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public boolean sigue(int idUsuario, int idEntidad) throws BusinessLogicException {
        try {
            return seguimientoDAO.existe(idUsuario, idEntidad);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF05.3
    @Override
    public void seguir(int idUsuario, int idEntidad) throws BusinessLogicException {
        try {
            SeguimientoEntidad seguimiento = new SeguimientoEntidad();
            Usuario usuario = new Usuario();
            usuario.setId(idUsuario);
            seguimiento.setUsuario(usuario);
            Entidad entidad = new Entidad();
            entidad.setId(idEntidad);
            seguimiento.setEntidad(entidad);
            seguimiento.setFecha(LocalDateTime.now().withNano(0));
            seguimientoDAO.insertar(seguimiento);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void dejarDeSeguir(int idUsuario, int idEntidad) throws BusinessLogicException {
        try {
            seguimientoDAO.eliminar(idUsuario, idEntidad);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }
}
