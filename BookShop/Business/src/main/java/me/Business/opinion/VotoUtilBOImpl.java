package me.Business.opinion;

import java.sql.SQLException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.Autorizacion;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.catalogo.ResenaDAO;
import me.DAO.catalogo.ResenaDAOImpl;
import me.DAO.catalogo.VotoUtilDAO;
import me.DAO.catalogo.VotoUtilDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.opinion.Resena;
import me.Model.entities.usuario.Usuario;

public class VotoUtilBOImpl implements VotoUtilBO {
    private final VotoUtilDAO votos;
    private final ResenaDAO resenas;
    private final LibroDAO libros;
    private final Autorizacion autorizacion;

    public VotoUtilBOImpl() { this(new VotoUtilDAOImpl(), new ResenaDAOImpl(), new LibroDAOImpl(), new UsuarioDAOImpl()); }

    public VotoUtilBOImpl(VotoUtilDAO votos, ResenaDAO resenas, LibroDAO libros, UsuarioDAO usuarios) {
        this.votos = votos;
        this.resenas = resenas;
        this.libros = libros;
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public boolean cambiarVoto(int idActor, int idResena, boolean marcar) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            Resena resena = resenas.leerParaActualizar(idResena);
            if (resena == null || resena.isEliminado() || !resena.isActivo())
                throw new RecursoNoEncontradoException("La resena no esta publicada.");
            Libro libro = libros.leer(resena.getLibro().getId());
            if (libro == null || !libro.isActivo()) throw new RecursoNoEncontradoException("El libro no esta disponible.");
            // Las referencias no prohiben votar la resena propia; la relacion impide duplicados.
            boolean cambio = votos.cambiarVoto(idActor, idResena, marcar, actor.getEmail());
            TransactionContext.commit();
            return cambio;
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo guardar el voto util.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }
}
