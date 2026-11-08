package me.Business.usuario;

import java.sql.SQLException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.DAO.catalogo.AutorDAO;
import me.DAO.catalogo.AutorDAOImpl;
import me.DAO.catalogo.CategoriaDAO;
import me.DAO.catalogo.CategoriaDAOImpl;
import me.DAO.catalogo.EditorialDAO;
import me.DAO.catalogo.EditorialDAOImpl;
import me.DAO.usuario.SeguimientoDAO;
import me.DAO.usuario.SeguimientoDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.usuario.ConsultaSeguimientosDTO;
import me.Model.dto.usuario.TipoSeguimiento;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.usuario.Usuario;

public class SeguimientoBOImpl implements SeguimientoBO {
    private final SeguimientoDAO seguimientoDAO;
    private final AutorDAO autorDAO;
    private final CategoriaDAO categoriaDAO;
    private final EditorialDAO editorialDAO;
    private final Autorizacion autorizacion;

    public SeguimientoBOImpl() {
        this(new SeguimientoDAOImpl(), new AutorDAOImpl(), new CategoriaDAOImpl(), new EditorialDAOImpl(),
                new UsuarioDAOImpl());
    }

    public SeguimientoBOImpl(SeguimientoDAO seguimientoDAO, AutorDAO autorDAO, CategoriaDAO categoriaDAO,
                             EditorialDAO editorialDAO, UsuarioDAO usuarios) {
        this.seguimientoDAO = seguimientoDAO;
        this.autorDAO = autorDAO;
        this.categoriaDAO = categoriaDAO;
        this.editorialDAO = editorialDAO;
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public void guardar(int idActor, TipoSeguimiento tipo, int idReferencia) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        validarReferencia(tipo, idReferencia);
        try {
            boolean disponible;
            switch (tipo) {
                case AUTOR: {
                    Autor autor = autorDAO.leer(idReferencia);
                    if (autor == null || autor.isEliminado()) throw new RecursoNoEncontradoException("El autor no existe.");
                    disponible = autor.estaDisponible();
                    break;
                }
                case CATEGORIA: {
                    Categoria categoria = categoriaDAO.leer(idReferencia);
                    if (categoria == null || categoria.isEliminado()) {
                        throw new RecursoNoEncontradoException("La categoria no existe.");
                    }
                    disponible = categoria.estaDisponible();
                    break;
                }
                case EDITORIAL: {
                    Editorial editorial = editorialDAO.leer(idReferencia);
                    if (editorial == null || editorial.isEliminado()) {
                        throw new RecursoNoEncontradoException("La editorial no existe.");
                    }
                    disponible = editorial.estaDisponible();
                    break;
                }
                default:
                    throw new ValidacionException("tipo", "Selecciona un tipo de seguimiento valido.");
            }
            if (!disponible) throw new ConflictoException("La referencia no esta disponible para seguirla.");
            seguimientoDAO.guardar(idActor, tipo, idReferencia, actor.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el seguimiento.", e);
        }
    }

    @Override
    public boolean retirar(int idActor, TipoSeguimiento tipo, int idReferencia) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        validarReferencia(tipo, idReferencia);
        try {
            return seguimientoDAO.retirar(idActor, tipo, idReferencia, actor.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo retirar el seguimiento.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> listar(int idActor, ConsultaSeguimientosDTO consulta)
            throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        if (consulta == null) consulta = ConsultaSeguimientosDTO.porDefecto();
        if (consulta.tipo() == null) throw new ValidacionException("tipo", "Selecciona una pestaña de seguimientos.");
        if (consulta.orden() == null) throw new ValidacionException("orden", "Selecciona un orden para los seguimientos.");
        if (consulta.pagina() < 1 || consulta.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        try {
            return seguimientoDAO.listarPorUsuario(idActor, consulta.tipo(), consulta.orden(),
                    consulta.pagina(), consulta.tamanioPagina());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los seguimientos.", e);
        }
    }

    private void validarReferencia(TipoSeguimiento tipo, int idReferencia) throws ValidacionException {
        if (tipo == null) throw new ValidacionException("tipo", "Selecciona un tipo de seguimiento.");
        if (idReferencia < 1) throw new ValidacionException("idReferencia", "Selecciona una referencia valida.");
    }
}
