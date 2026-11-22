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
import java.time.LocalDateTime;
import java.util.List;

public class SeguimientoBLImpl implements SeguimientoBL {

    private SeguimientoDAO seguimientoDAO = new SeguimientoDAOImpl();

    // RF05.4: búsqueda por nombre y orden por nombre o por fecha de seguimiento ("fecha": el más reciente primero).
    // Lo inactivo no se muestra (RF04.2).
    @Override
    public ResultadoPagina<Entidad> listar(int idUsuario, TipoEntidad tipo, String texto, String orden, int pagina, int tamano) throws BusinessLogicException {
        try {
            int total = seguimientoDAO.contar(idUsuario, tipo, texto);
            List<Entidad> entidades = seguimientoDAO.listarPagina(idUsuario, tipo, texto, orden, pagina, tamano);
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
