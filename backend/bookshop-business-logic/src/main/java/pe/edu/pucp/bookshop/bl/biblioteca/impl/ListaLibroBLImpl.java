package pe.edu.pucp.bookshop.bl.biblioteca.impl;

import pe.edu.pucp.bookshop.bl.biblioteca.ListaLibroBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.biblioteca.ListaLibroDAO;
import pe.edu.pucp.bookshop.dao.biblioteca.impl.ListaLibroDAOImpl;
import pe.edu.pucp.bookshop.model.biblioteca.TipoLista;

import java.sql.SQLException;
import java.util.List;

public class ListaLibroBLImpl implements ListaLibroBL {

    private ListaLibroDAO listaDAO = new ListaLibroDAOImpl();

    // En qué listas del cliente está el libro (botones del detalle)
    @Override
    public List<TipoLista> listarListas(int idUsuario, int idLibro) throws BusinessLogicException {
        try {
            return listaDAO.listarListas(idUsuario, idLibro);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF06.1–2: las dos listas son independientes
    @Override
    public void agregar(int idUsuario, int idLibro, TipoLista lista) throws BusinessLogicException {
        try {
            listaDAO.insertar(idUsuario, idLibro, lista);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void quitar(int idUsuario, int idLibro, TipoLista lista) throws BusinessLogicException {
        try {
            listaDAO.eliminar(idUsuario, idLibro, lista);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }
}
