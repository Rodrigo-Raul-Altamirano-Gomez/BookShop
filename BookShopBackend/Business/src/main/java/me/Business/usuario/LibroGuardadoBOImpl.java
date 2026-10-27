package me.Business.usuario;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import me.Business.catalogo.FiltrosCatalogo;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
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
    private final LibroGuardadoDAO guardados;
    private final LibroDAO libros;
    private final Clock reloj;
    private final Autorizacion autorizacion;

    public LibroGuardadoBOImpl() {
        this(new LibroGuardadoDAOImpl(), new LibroDAOImpl(), new UsuarioDAOImpl(),
                Clock.system(ZoneId.of("America/Lima")));
    }

    public LibroGuardadoBOImpl(LibroGuardadoDAO guardados, LibroDAO libros, UsuarioDAO usuarios, Clock reloj) {
        this.guardados = Objects.requireNonNull(guardados);
        this.libros = Objects.requireNonNull(libros);
        this.reloj = Objects.requireNonNull(reloj).withZone(ZoneId.of("America/Lima"));
        this.autorizacion = new Autorizacion(Objects.requireNonNull(usuarios));
    }

    @Override
    public void guardar(int idActor, int idLibro, boolean favorito) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        if (idLibro < 1) throw new ValidacionException("idLibro", "Selecciona un libro valido.");
        try {
            Libro libro = libros.leer(idLibro);
            if (libro == null || libro.isEliminado()) throw new RecursoNoEncontradoException("El libro no existe.");
            if (!libro.isActivo()) throw new ConflictoException("El libro no esta disponible.");
            guardados.guardar(idActor, idLibro, favorito, actor.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el libro en la lista.", e);
        }
    }

    @Override
    public boolean retirar(int idActor, int idLibro, boolean favorito) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        if (idLibro < 1) throw new ValidacionException("idLibro", "Selecciona un libro valido.");
        try {
            // Tambien puede retirarse una relacion propia cuyo libro ya no este disponible.
            return guardados.retirar(idActor, idLibro, favorito, actor.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo retirar el libro de la lista.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> listar(int idActor, boolean favorito, FiltroLibroDTO filtro)
            throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        FiltroLibroDTO validos = FiltrosCatalogo.validar(filtro);
        FiltroLibroDTO visibles = new FiltroLibroDTO(validos.titulo(), validos.idsCategorias(), validos.idsAutores(),
                validos.idsEditoriales(), validos.idiomas(), validos.precioMinimo(), validos.precioMaximo(),
                true, validos.soloNuevos(), validos.orden(), validos.pagina(), validos.tamanioPagina());
        try {
            return libros.buscarGuardados(idActor, favorito, visibles, LocalDateTime.now(reloj));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros guardados.", e);
        }
    }
}
