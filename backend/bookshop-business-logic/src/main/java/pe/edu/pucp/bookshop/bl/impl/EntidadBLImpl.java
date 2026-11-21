package pe.edu.pucp.bookshop.bl.impl;

import pe.edu.pucp.bookshop.bl.EntidadBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.NoEncontradoException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.EntidadDAO;
import pe.edu.pucp.bookshop.dao.impl.EntidadDAOImpl;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.TipoEntidad;
import pe.edu.pucp.bookshop.model.dto.EntidadAdmin;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

import java.sql.SQLException;
import java.util.List;

public class EntidadBLImpl implements EntidadBL {

    private EntidadDAO entidadDAO = new EntidadDAOImpl();

    // RF05.1: búsqueda por nombre y orden por nombre, solo las activas
    @Override
    public ResultadoPagina<Entidad> listar(TipoEntidad tipo, String texto, String orden, int pagina, int tamano) throws BusinessLogicException {
        try {
            boolean descendente = "nombre-desc".equals(orden);
            int total = entidadDAO.contar(tipo, texto, true);
            List<Entidad> entidades = entidadDAO.listarPagina(tipo, texto, true, descendente, pagina, tamano);
            return new ResultadoPagina<>(total, entidades);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Entidad obtener(int id) throws BusinessLogicException {
        try {
            Entidad entidad = entidadDAO.obtener(id);
            if (entidad == null) {
                throw new NoEncontradoException("No encontramos lo que buscas.");
            }
            return entidad;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // estado: ACTIVO, INACTIVO o null (todos)
    @Override
    public ResultadoPagina<EntidadAdmin> listarAdmin(TipoEntidad tipo, String texto, String estado, int pagina, int tamano) throws BusinessLogicException {
        try {
            Boolean activo = null;
            if ("ACTIVO".equals(estado)) {
                activo = true;
            }
            if ("INACTIVO".equals(estado)) {
                activo = false;
            }
            int total = entidadDAO.contar(tipo, texto, activo);
            List<EntidadAdmin> filas = entidadDAO.listarPaginaAdmin(tipo, texto, activo, pagina, tamano);
            return new ResultadoPagina<>(total, filas);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public List<Entidad> listarActivas(TipoEntidad tipo) throws BusinessLogicException {
        try {
            return entidadDAO.listarActivas(tipo);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Entidad insertar(Entidad entidad) throws BusinessLogicException {
        try {
            entidad.setId(entidadDAO.insertar(entidad));
            return entidad;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void modificar(Entidad entidad) throws BusinessLogicException {
        try {
            entidadDAO.modificar(entidad);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RN03: no se borra, se desactiva
    @Override
    public void cambiarActivo(int id, boolean activo) throws BusinessLogicException {
        try {
            entidadDAO.cambiarActivo(id, activo);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }
}
