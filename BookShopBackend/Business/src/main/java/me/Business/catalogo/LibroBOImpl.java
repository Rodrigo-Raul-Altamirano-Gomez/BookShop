package me.Business.catalogo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.AutorDAO;
import me.DAO.catalogo.AutorDAOImpl;
import me.DAO.catalogo.CategoriaDAO;
import me.DAO.catalogo.CategoriaDAOImpl;
import me.DAO.catalogo.EditorialDAO;
import me.DAO.catalogo.EditorialDAOImpl;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DatosLibroDTO;
import me.Model.dto.catalogo.DetalleLibroDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.GestionLibroDTO;
import me.Model.dto.catalogo.RegistroLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.Usuario;

public class LibroBOImpl implements LibroBO {

    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final AutorDAO autorDAO = new AutorDAOImpl();
    private final CategoriaDAO categoriaDAO = new CategoriaDAOImpl();
    private final EditorialDAO editorialDAO = new EditorialDAOImpl();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    @Override
    public int registrar(int idActor, RegistroLibroDTO registro) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (registro == null)
                throw new ValidacionException("registro", "Complete los datos del libro.");
            DatosLibroDTO datos = registro.datos();

            if (registro.precioBase() == null || registro.precioBase().signum() <= 0)
                throw new ValidacionException("precioBase", "Ingrese un precio positivo.");
            BigDecimal precio = registro.precioBase().setScale(2, java.math.RoundingMode.HALF_UP);
            if (registro.stockInicial() < 0)
                throw new ValidacionException("stockInicial", "El stock no puede ser negativo.");
            if (libroDAO.existeIsbn(datos.isbn().strip(), 0))
                throw new ValidacionException("isbn", "El ISBN ya esta registrado.");
            Autor autor = autorDAO.leer(registro.idAutor());
            Categoria categoria = categoriaDAO.leer(registro.idCategoria());
            Editorial editorial = editorialDAO.leer(registro.idEditorial());
            if (autor == null || !autor.estaDisponible() || categoria == null || !categoria.estaDisponible()
                || editorial == null || !editorial.estaDisponible())
                throw new NegocioException("Las referencias seleccionadas ya no estan disponibles.");
            Libro libro = new Libro();
            libro.setTitulo(datos.titulo().strip());
            libro.setIdioma(datos.idioma().strip());
            libro.setSinopsis(datos.sinopsis().strip());
            libro.setProveedor(datos.proveedor().strip());
            libro.setIsbn(datos.isbn().strip());
            libro.setAnioEdicion(datos.anioEdicion());
            libro.setNumeroPaginas(datos.numeroPaginas());
            libro.setPeso(datos.peso());
            libro.setAlto(datos.alto());
            libro.setAncho(datos.ancho());
            libro.setSaga(datos.saga());
            libro.setNumeroTomo(datos.numeroTomo());
            libro.setAutor(autor);
            libro.setCategoria(categoria);
            libro.setEditorial(editorial);
            libro.setPrecioBase(precio);
            libro.setStockFisico(registro.stockInicial());
            libro.setPortada(registro.portada());
            libro.setUsuarioCreacion(actor.getEmail());
            libro.setFechaCreacion(LocalDateTime.now());
            libroDAO.insertar(libro);
            TransactionContext.commit();
            return libro.getId();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo registrar el libro.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public void modificar(int idActor, int id, DatosLibroDTO datos) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);

            Libro libro = libroDAO.leer(id);
            if (libro == null || libro.isEliminado())
                throw new RecursoNoEncontradoException("El libro no está disponible.");
            if (libroDAO.existeIsbn(datos.isbn().strip(), id))
                throw new ValidacionException("isbn", "El ISBN ya esta registrado.");
            libro.setTitulo(datos.titulo().strip());
            libro.setIdioma(datos.idioma().strip());
            libro.setSinopsis(datos.sinopsis().strip());
            libro.setProveedor(datos.proveedor().strip());
            libro.setIsbn(datos.isbn().strip());
            libro.setAnioEdicion(datos.anioEdicion());
            libro.setNumeroPaginas(datos.numeroPaginas());
            libro.setPeso(datos.peso());
            libro.setAlto(datos.alto());
            libro.setAncho(datos.ancho());
            libro.setSaga(datos.saga());
            libro.setNumeroTomo(datos.numeroTomo());
            libro.setUsuarioActualizacion(actor.getEmail());
            libroDAO.modificar(libro);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron modificar los datos del libro.", e);
        }
    }

    @Override
    public void cambiarPrecio(int idActor, int id, BigDecimal precio) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (precio == null || precio.signum() <= 0)
                throw new ValidacionException("precioBase", "Ingrese un precio positivo.");
            BigDecimal valido = precio.setScale(2, java.math.RoundingMode.HALF_UP);
            Libro libro = libroDAO.leer(id);
            if (libro == null || libro.isEliminado())
                throw new RecursoNoEncontradoException("El libro no está disponible.");
            if (!libroDAO.cambiarPrecio(id, valido, actor.getEmail()))
                throw new NegocioException("El libro cambio; vuelva a consultarlo.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar el precio.", e);
        }
    }

    @Override
    public void cambiarStock(int idActor, int id, int stock) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (stock < 0)
                throw new ValidacionException("stock", "El stock no puede ser negativo.");
            Libro libro = libroDAO.leer(id);
            if (libro == null || libro.isEliminado())
                throw new RecursoNoEncontradoException("El libro no está disponible.");
            if (!libroDAO.cambiarStock(id, stock, actor.getEmail()))
                throw new NegocioException("El libro cambio; vuelva a consultarlo.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar el stock.", e);
        }
    }

    @Override
    public void cambiarImagen(int idActor, int id, String imagen) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Libro libro = libroDAO.leer(id);
            if (libro == null || libro.isEliminado())
                throw new RecursoNoEncontradoException("El libro no está disponible.");
            if (!libroDAO.cambiarImagen(id, imagen, actor.getEmail()))
                throw new NegocioException("El libro cambio; vuelva a consultarlo.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar la portada.", e);
        }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Libro libro = libroDAO.leer(id);
            if (libro == null || libro.isEliminado())
                throw new RecursoNoEncontradoException("El libro no está disponible.");
            int vetosReferencias = libro.getNumeroVetos() - (libro.isVetadoDirectamente() ? 1 : 0);
            if (activo && vetosReferencias > 0)
                throw new NegocioException("Primero active las referencias vetadas del libro.");
            if (libro.isVetadoDirectamente() == !activo)
                return;
            if (!libroDAO.cambiarVetoDirecto(id, !activo, actor.getEmail()))
                throw new NegocioException("El libro cambio; vuelva a consultarlo.");
            if (activo) {
                Libro actualizado = libroDAO.leer(id);
                if (actualizado == null || !actualizado.isActivo())
                    throw new NegocioException("Una referencia fue vetada; el libro permanece inactivo.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo cambiar el estado del libro.", e);
        }
    }

    @Override
    public void eliminar(int idActor, int id) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            Libro libro = libroDAO.leer(id);
            if (libro == null || libro.isEliminado())
                throw new RecursoNoEncontradoException("El libro no está disponible.");
            if (!libroDAO.eliminar(id, actor.getEmail()))
                throw new NegocioException("El libro cambio; vuelva a consultarlo.");
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar el libro.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscar(FiltroLibroDTO filtro) throws
        NegocioException {
        FiltroLibroDTO valido = validarFiltro(filtro);
        try {
            return libroDAO.buscar(valido, LocalDateTime.now());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el catalogo.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorAutor(int id, FiltroLibroDTO filtro)
        throws NegocioException {
        FiltroLibroDTO valido = validarFiltro(filtro);
        try {
            Autor autor = autorDAO.leer(id);
            if (autor == null || autor.isEliminado())
                throw new RecursoNoEncontradoException("El autor no esta disponible.");
            return libroDAO.buscarPorAutor(id, valido, LocalDateTime.now());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros del autor.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorCategoria(int id, FiltroLibroDTO
        filtro) throws NegocioException {
        FiltroLibroDTO valido = validarFiltro(filtro);
        try {
            Categoria categoria = categoriaDAO.leer(id);
            if (categoria == null || categoria.isEliminado())
                throw new RecursoNoEncontradoException("La categoria no esta disponible.");
            return libroDAO.buscarPorCategoria(id, valido, LocalDateTime.now());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros de la categoria.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorEditorial(int id, FiltroLibroDTO
        filtro) throws NegocioException {
        FiltroLibroDTO valido = validarFiltro(filtro);
        try {
            Editorial editorial = editorialDAO.leer(id);
            if (editorial == null || editorial.isEliminado())
                throw new RecursoNoEncontradoException("La editorial no esta disponible.");
            return libroDAO.buscarPorEditorial(id, valido, LocalDateTime.now());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros de la editorial.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<String> buscarIdiomas(String nombre, int pagina, int tamanio)
        throws NegocioException {
        if (pagina < 1 || tamanio < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        try {
            return libroDAO.buscarIdiomas(nombre, true, pagina, tamanio);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los idiomas.", e);
        }
    }

    @Override
    public DetalleLibroDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        try {
            DetalleLibroDTO detalle = libroDAO.obtenerDetalle(id, idActor, LocalDateTime.now());
            if (detalle == null)
                throw new RecursoNoEncontradoException("El libro no esta disponible.");
            return detalle;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el libro.", e);
        }
    }

    @Override
    public GestionLibroDTO obtenerGestion(int id) throws NegocioException {
        try {
            GestionLibroDTO gestion = libroDAO.obtenerGestion(id);
            if (gestion == null)
                throw new RecursoNoEncontradoException("El libro no esta disponible.");
            return gestion;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la gestion del libro.", e);
        }
    }

    @Override
    public List<DetalleLibroDTO> comparar(int idActor, int primero, int segundo) throws NegocioException {
        if (primero == segundo)
            throw new ValidacionException("libros", "Seleccione dos libros distintos.");
        return List.of(obtenerDetalle(idActor, primero), obtenerDetalle(idActor, segundo));
    }

    public static FiltroLibroDTO validarFiltro(FiltroLibroDTO filtro) throws ValidacionException {
        if (filtro == null)
            filtro = FiltroLibroDTO.porDefecto();
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        if (filtro.precioMinimo() != null && filtro.precioMinimo().signum() < 0)
            throw new ValidacionException("precioMinimo", "El precio no puede ser negativo.");
        if (filtro.precioMaximo() != null && filtro.precioMaximo().signum() < 0)
            throw new ValidacionException("precioMaximo", "El precio no puede ser negativo.");
        if (filtro.precioMinimo() != null && filtro.precioMaximo() != null &&
            filtro.precioMinimo().compareTo(filtro.precioMaximo()) > 0)
            throw new ValidacionException("precioMaximo", "El máximo no puede ser menor que el mínimo.");
        return filtro;
    }
}
