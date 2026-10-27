package me.Business.usuario;

import java.sql.SQLException;
import java.util.Objects;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.Business.Validaciones;
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
    private final SeguimientoDAO seguimientos;
    private final AutorDAO autores;
    private final CategoriaDAO categorias;
    private final EditorialDAO editoriales;
    private final Autorizacion autorizacion;

    public SeguimientoBOImpl() {
        this(new SeguimientoDAOImpl(), new AutorDAOImpl(), new CategoriaDAOImpl(), new EditorialDAOImpl(),
                new UsuarioDAOImpl());
    }

    public SeguimientoBOImpl(SeguimientoDAO seguimientos, AutorDAO autores, CategoriaDAO categorias,
                             EditorialDAO editoriales, UsuarioDAO usuarios) {
        this.seguimientos = Objects.requireNonNull(seguimientos);
        this.autores = Objects.requireNonNull(autores);
        this.categorias = Objects.requireNonNull(categorias);
        this.editoriales = Objects.requireNonNull(editoriales);
        this.autorizacion = new Autorizacion(Objects.requireNonNull(usuarios));
    }

    @Override
    public void guardar(int idActor, TipoSeguimiento tipo, int idReferencia) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        validarReferencia(tipo, idReferencia);
        try {
            boolean disponible;
            switch (tipo) {
                case AUTOR: {
                    Autor autor = autores.leer(idReferencia);
                    if (autor == null || autor.isEliminado()) throw new RecursoNoEncontradoException("El autor no existe.");
                    disponible = autor.estaDisponible();
                    break;
                }
                case CATEGORIA: {
                    Categoria categoria = categorias.leer(idReferencia);
                    if (categoria == null || categoria.isEliminado()) {
                        throw new RecursoNoEncontradoException("La categoria no existe.");
                    }
                    disponible = categoria.estaDisponible();
                    break;
                }
                case EDITORIAL: {
                    Editorial editorial = editoriales.leer(idReferencia);
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
            seguimientos.guardar(idActor, tipo, idReferencia, actor.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el seguimiento.", e);
        }
    }

    @Override
    public boolean retirar(int idActor, TipoSeguimiento tipo, int idReferencia) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        validarReferencia(tipo, idReferencia);
        try {
            return seguimientos.retirar(idActor, tipo, idReferencia, actor.getEmail());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo retirar el seguimiento.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> listar(int idActor, ConsultaSeguimientosDTO consulta)
            throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        if (consulta == null) consulta = new ConsultaSeguimientosDTO();
        if (consulta.tipo() == null) throw new ValidacionException("tipo", "Selecciona una pestaña de seguimientos.");
        if (consulta.orden() == null) throw new ValidacionException("orden", "Selecciona un orden para los seguimientos.");
        Validaciones.pagina(consulta.pagina(), consulta.tamanioPagina());
        try {
            return seguimientos.listarPorUsuario(idActor, consulta.tipo(), consulta.orden(),
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
