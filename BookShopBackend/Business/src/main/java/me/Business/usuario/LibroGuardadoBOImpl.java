package me.Business.usuario;

import java.sql.SQLException;
import java.time.LocalDateTime;
import me.Business.exceptions.NegocioException;
import me.Business.catalogo.LibroBOImpl;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.LibroGuardadoDAO;
import me.DAO.usuario.LibroGuardadoDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.Usuario;

public class LibroGuardadoBOImpl implements LibroGuardadoBO {

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final LibroGuardadoDAO libroGuardadoDAO = new LibroGuardadoDAOImpl();
    private final LibroDAO libroDAO = new LibroDAOImpl();

    @Override
    public void guardar(int idActor, int idLibro, boolean favorito) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Libro libro = libroDAO.leer(idLibro);
            if (libro == null || libro.isEliminado())
                throw new RecursoNoEncontradoException("El libro no existe.");
            if (!libro.isActivo())
                throw new NegocioException("El libro no esta disponible.");
            libroGuardadoDAO.guardar(idActor, idLibro, favorito, actor.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el libro en la lista.", e);
        }
    }

    @Override
    public boolean retirar(int idActor, int idLibro, boolean favorito) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            // Tambien puede retirarse una relacion propia cuyo libro ya no este disponible.
            return libroGuardadoDAO.retirar(idActor, idLibro, favorito, actor.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo retirar el libro de la lista.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> listar(int idActor, boolean favorito, FiltroLibroDTO filtro)
        throws NegocioException {
        FiltroLibroDTO validos = LibroBOImpl.validarFiltro(filtro);
        FiltroLibroDTO visibles = new FiltroLibroDTO(validos.titulo(), validos.idsCategorias(),
            validos.idsAutores(), validos.idsEditoriales(), validos.idiomas(), validos.precioMinimo(),
            validos.precioMaximo(), true, validos.soloNuevos(), validos.orden(), validos.pagina(),
            validos.tamanioPagina());
        try {
            return libroDAO.buscarGuardados(idActor, favorito, visibles, LocalDateTime.now());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros guardados.", e);
        }
    }

}
