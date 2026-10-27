package me.Business.catalogo;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
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
    private final LibroDAO libros;
    private final AutorDAO autores;
    private final CategoriaDAO categorias;
    private final EditorialDAO editoriales;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public LibroBOImpl() {
        this(new LibroDAOImpl(), new AutorDAOImpl(), new CategoriaDAOImpl(), new EditorialDAOImpl(),
                new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public LibroBOImpl(LibroDAO libros, AutorDAO autores, CategoriaDAO categorias, EditorialDAO editoriales,
                       UsuarioDAO usuarios, Clock reloj) {
        this.libros = libros;
        this.autores = autores;
        this.categorias = categorias;
        this.editoriales = editoriales;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public int registrar(int idActor, RegistroLibroDTO registro) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (registro == null) throw new ValidacionException("registro", "Complete los datos del libro.");
        DatosLibroDTO datos = validarDatos(registro.datos());
        BigDecimal precio = FiltrosCatalogo.importe(registro.precioBase(), "precioBase", false);
        if (registro.stockInicial() < 0) throw new ValidacionException("stockInicial", "El stock no puede ser negativo.");
        if (registro.idAutor() < 1 || registro.idCategoria() < 1 || registro.idEditorial() < 1)
            throw new ValidacionException("referencias", "Seleccione autor, categoria y editorial.");
        Validaciones.imagen(registro.portada());
        try {
            if (libros.existeIsbn(datos.isbn(), 0)) throw new ValidacionException("isbn", "El ISBN ya esta registrado.");
            Autor autor = autores.leerParaActualizar(registro.idAutor());
            Categoria categoria = categorias.leerParaActualizar(registro.idCategoria());
            Editorial editorial = editoriales.leerParaActualizar(registro.idEditorial());
            if (autor == null || !autor.estaDisponible() || categoria == null || !categoria.estaDisponible()
                    || editorial == null || !editorial.estaDisponible())
                throw new ConflictoException("Las referencias seleccionadas ya no estan disponibles.");
            Libro libro = new Libro();
            asignarDatos(libro, datos);
            libro.setAutor(autor);
            libro.setCategoria(categoria);
            libro.setEditorial(editorial);
            libro.setPrecioBase(precio);
            libro.setStockFisico(registro.stockInicial());
            if (registro.portada() != null) {
                libro.setPortada(registro.portada().contenido());
                libro.setTipoPortada(registro.portada().tipoMime());
            }
            libro.setUsuarioCreacion(actor.getEmail());
            libro.setFechaCreacion(LocalDateTime.now(reloj));
            libros.insertar(libro);
            TransactionContext.commit();
            return libro.getId();
        } catch (SQLException e) {
            TransactionContext.rollback();
            if ("23505".equals(e.getSQLState()) || e.getErrorCode() == 1062)
                throw new ConflictoException("Otro registro ya utiliza ese ISBN.");
            throw new PersistenciaException("No se pudo registrar el libro.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public void modificar(int idActor, int id, DatosLibroDTO datos) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        DatosLibroDTO validos = validarDatos(datos);
        try {
            Libro libro = existente(id);
            if (libros.existeIsbn(validos.isbn(), id)) throw new ValidacionException("isbn", "El ISBN ya esta registrado.");
            asignarDatos(libro, validos);
            libro.setUsuarioActualizacion(actor.getEmail());
            libros.modificar(libro);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()) || e.getErrorCode() == 1062)
                throw new ConflictoException("Otro registro ya utiliza ese ISBN.");
            throw new PersistenciaException("No se pudieron modificar los datos del libro.", e);
        }
    }

    @Override
    public void cambiarPrecio(int idActor, int id, BigDecimal precio) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        BigDecimal valido = FiltrosCatalogo.importe(precio, "precioBase", false);
        try {
            existente(id);
            if (!libros.cambiarPrecio(id, valido, actor.getEmail())) throw new ConflictoException("El libro cambio; vuelva a consultarlo.");
        } catch (SQLException e) { throw new PersistenciaException("No se pudo cambiar el precio.", e); }
    }

    @Override
    public void cambiarStock(int idActor, int id, int stock) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (stock < 0) throw new ValidacionException("stock", "El stock no puede ser negativo.");
        try {
            existente(id);
            if (!libros.cambiarStock(id, stock, actor.getEmail())) throw new ConflictoException("El libro cambio; vuelva a consultarlo.");
        } catch (SQLException e) { throw new PersistenciaException("No se pudo cambiar el stock.", e); }
    }

    @Override
    public void cambiarImagen(int idActor, int id, ImagenDTO imagen) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        Validaciones.imagen(imagen);
        try {
            existente(id);
            if (!libros.cambiarImagen(id, imagen, actor.getEmail())) throw new ConflictoException("El libro cambio; vuelva a consultarlo.");
        } catch (SQLException e) { throw new PersistenciaException("No se pudo cambiar la portada.", e); }
    }

    @Override
    public void cambiarActivo(int idActor, int id, boolean activo) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            Libro libro = existente(id);
            int vetosReferencias = libro.getNumeroVetos() - (libro.isVetadoDirectamente() ? 1 : 0);
            if (activo && vetosReferencias > 0) throw new ConflictoException("Primero active las referencias vetadas del libro.");
            if (libro.isVetadoDirectamente() == !activo) return;
            if (!libros.cambiarVetoDirecto(id, !activo, actor.getEmail()))
                throw new ConflictoException("El libro cambio; vuelva a consultarlo.");
            if (activo && !existente(id).isActivo()) throw new ConflictoException("Una referencia fue vetada; el libro permanece inactivo.");
        } catch (SQLException e) { throw new PersistenciaException("No se pudo cambiar el estado del libro.", e); }
    }

    @Override
    public void eliminar(int idActor, int id) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        try {
            existente(id);
            if (!libros.eliminar(id, actor.getEmail())) throw new ConflictoException("El libro cambio; vuelva a consultarlo.");
        } catch (SQLException e) { throw new PersistenciaException("No se pudo eliminar el libro.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscar(int idActor, FiltroLibroDTO filtro) throws NegocioException {
        FiltroLibroDTO valido = filtroPermitido(idActor, filtro);
        try { return libros.buscar(valido, LocalDateTime.now(reloj)); }
        catch (SQLException e) { throw new PersistenciaException("No se pudo consultar el catalogo.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorAutor(int idActor, int id, FiltroLibroDTO filtro) throws NegocioException {
        FiltroLibroDTO valido = filtroPermitido(idActor, filtro);
        try {
            Autor autor = autores.leer(id);
            if (autor == null || autor.isEliminado()) throw new RecursoNoEncontradoException("El autor no esta disponible.");
            if (!autor.isActivo()) autorizacion.administrador(idActor);
            return libros.buscarPorAutor(id, valido, LocalDateTime.now(reloj));
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los libros del autor.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorCategoria(int idActor, int id, FiltroLibroDTO filtro) throws NegocioException {
        FiltroLibroDTO valido = filtroPermitido(idActor, filtro);
        try {
            Categoria categoria = categorias.leer(id);
            if (categoria == null || categoria.isEliminado()) throw new RecursoNoEncontradoException("La categoria no esta disponible.");
            if (!categoria.isActivo()) autorizacion.administrador(idActor);
            return libros.buscarPorCategoria(id, valido, LocalDateTime.now(reloj));
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los libros de la categoria.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorEditorial(int idActor, int id, FiltroLibroDTO filtro) throws NegocioException {
        FiltroLibroDTO valido = filtroPermitido(idActor, filtro);
        try {
            Editorial editorial = editoriales.leer(id);
            if (editorial == null || editorial.isEliminado()) throw new RecursoNoEncontradoException("La editorial no esta disponible.");
            if (!editorial.isActivo()) autorizacion.administrador(idActor);
            return libros.buscarPorEditorial(id, valido, LocalDateTime.now(reloj));
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los libros de la editorial.", e); }
    }

    @Override
    public ResultadoPaginadoDTO<String> buscarIdiomas(int idActor, String nombre, int pagina, int tamanio) throws NegocioException {
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        Validaciones.pagina(pagina, tamanio);
        String valido = Validaciones.opcional(nombre, "idioma", 100);
        try { return libros.buscarIdiomas(valido, true, pagina, tamanio); }
        catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los idiomas.", e); }
    }

    @Override
    public DetalleLibroDTO obtenerDetalle(int idActor, int id) throws NegocioException {
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        try {
            DetalleLibroDTO detalle = libros.obtenerDetalle(id, idActor, LocalDateTime.now(reloj));
            if (detalle == null) throw new RecursoNoEncontradoException("El libro no esta disponible.");
            if (!detalle.libro().activo()) autorizacion.administrador(idActor);
            return detalle;
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar el libro.", e); }
    }

    @Override
    public GestionLibroDTO obtenerGestion(int idActor, int id) throws NegocioException {
        autorizacion.administrador(idActor);
        try {
            GestionLibroDTO gestion = libros.obtenerGestion(id);
            if (gestion == null) throw new RecursoNoEncontradoException("El libro no esta disponible.");
            return gestion;
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar la gestion del libro.", e); }
    }

    @Override
    public List<DetalleLibroDTO> comparar(int idActor, int primero, int segundo) throws NegocioException {
        if (primero < 1 || segundo < 1 || primero == segundo)
            throw new ValidacionException("libros", "Seleccione dos libros distintos.");
        return List.of(obtenerDetalle(idActor, primero), obtenerDetalle(idActor, segundo));
    }

    private FiltroLibroDTO filtroPermitido(int idActor, FiltroLibroDTO filtro) throws NegocioException {
        FiltroLibroDTO valido = FiltrosCatalogo.validar(filtro);
        if (idActor != 0) autorizacion.usuarioActivo(idActor);
        if (!valido.soloActivos()) autorizacion.administrador(idActor);
        return valido;
    }

    private Libro existente(int id) throws SQLException, NegocioException {
        Libro libro = libros.leer(id);
        if (libro == null || libro.isEliminado()) throw new RecursoNoEncontradoException("El libro no esta disponible.");
        return libro;
    }

    private DatosLibroDTO validarDatos(DatosLibroDTO datos) throws ValidacionException {
        if (datos == null) throw new ValidacionException("datos", "Complete los datos del libro.");
        String sinopsis = Validaciones.texto(datos.sinopsis(), "sinopsis", 65535);
        if (sinopsis.getBytes(StandardCharsets.UTF_8).length > 65535)
            throw new ValidacionException("sinopsis", "La sinopsis es demasiado extensa.");
        if (datos.anioEdicion() < 1 || datos.anioEdicion() > 9999)
            throw new ValidacionException("anioEdicion", "Ingrese un anio entre 1 y 9999.");
        if (datos.numeroPaginas() < 1) throw new ValidacionException("numeroPaginas", "El numero de paginas debe ser positivo.");
        medida(datos.peso(), "peso", 3, "9999999.999");
        medida(datos.alto(), "alto", 2, "99999999.99");
        medida(datos.ancho(), "ancho", 2, "99999999.99");
        String saga = Validaciones.opcional(datos.saga(), "saga", 150);
        if (datos.numeroTomo() != null && (datos.numeroTomo() < 1 || saga == null))
            throw new ValidacionException("numeroTomo", "El tomo debe ser positivo y pertenecer a una saga.");
        return new DatosLibroDTO(Validaciones.texto(datos.titulo(), "titulo", 255),
                Validaciones.texto(datos.idioma(), "idioma", 100), sinopsis,
                Validaciones.texto(datos.proveedor(), "proveedor", 150), Validaciones.texto(datos.isbn(), "isbn", 20),
                datos.anioEdicion(), datos.numeroPaginas(), datos.peso(), datos.alto(), datos.ancho(), saga, datos.numeroTomo());
    }

    private void medida(Double valor, String campo, int decimales, String maximo) throws ValidacionException {
        if (valor != null && (!Double.isFinite(valor) || valor <= 0
                || BigDecimal.valueOf(valor).compareTo(new BigDecimal(maximo)) > 0
                || BigDecimal.valueOf(valor).stripTrailingZeros().scale() > decimales))
            throw new ValidacionException(campo, "Ingrese una medida positiva con precision valida.");
    }

    private void asignarDatos(Libro libro, DatosLibroDTO datos) {
        libro.setTitulo(datos.titulo());
        libro.setIdioma(datos.idioma());
        libro.setSinopsis(datos.sinopsis());
        libro.setProveedor(datos.proveedor());
        libro.setIsbn(datos.isbn());
        libro.setAnioEdicion(datos.anioEdicion());
        libro.setNumeroPaginas(datos.numeroPaginas());
        libro.setPeso(datos.peso());
        libro.setAlto(datos.alto());
        libro.setAncho(datos.ancho());
        libro.setSaga(datos.saga());
        libro.setNumeroTomo(datos.numeroTomo());
    }
}
