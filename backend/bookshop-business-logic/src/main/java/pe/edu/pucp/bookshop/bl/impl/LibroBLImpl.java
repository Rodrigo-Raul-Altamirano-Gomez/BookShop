package pe.edu.pucp.bookshop.bl.impl;

import pe.edu.pucp.bookshop.bl.LibroBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.NoEncontradoException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.LibroDAO;
import pe.edu.pucp.bookshop.dao.impl.LibroDAOImpl;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.dto.ConteoLibros;
import pe.edu.pucp.bookshop.model.dto.FiltroLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;

import java.sql.SQLException;
import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LibroBLImpl implements LibroBL {

    private LibroDAO libroDAO = new LibroDAOImpl();
    // Orden alfabético en español (tildes y ñ)
    private Collator alfabetico = Collator.getInstance(Locale.of("es"));

    // RF07.6: el catálogo es la vista de resultados de todos los libros activos
    @Override
    public ResultadoLibros listarCatalogo(FiltroLibros filtro) throws BusinessLogicException {
        try {
            List<Libro> activos = libroDAO.listarActivos(LocalDate.now());
            return filtrar(activos, filtro, true);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF07.3: Novedades, del registrado más recientemente al más antiguo; sin selector de orden
    @Override
    public ResultadoLibros listarNovedades(FiltroLibros filtro) throws BusinessLogicException {
        try {
            List<Libro> activos = libroDAO.listarActivos(LocalDate.now());
            activos.sort((a, b) -> b.getFechaRegistro().compareTo(a.getFechaRegistro()));
            return filtrar(activos, filtro, false);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF05.2: los libros activos de un autor, una categoría o una editorial
    @Override
    public ResultadoLibros listarPorEntidad(int idEntidad, FiltroLibros filtro) throws BusinessLogicException {
        try {
            List<Libro> libros = new ArrayList<>();
            for (Libro libro : libroDAO.listarActivos(LocalDate.now())) {
                if (libro.getAutor().getId() == idEntidad || libro.getCategoria().getId() == idEntidad
                        || libro.getEditorial().getId() == idEntidad) {
                    libros.add(libro);
                }
            }
            return filtrar(libros, filtro, true);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Libro obtener(int id) throws BusinessLogicException {
        try {
            Libro libro = libroDAO.obtener(id, LocalDate.now());
            if (libro == null) {
                throw new NoEncontradoException("No encontramos lo que buscas.");
            }
            return libro;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // estado: ACTIVO, INACTIVO o null (todos)
    @Override
    public ResultadoPagina<Libro> listarAdmin(String texto, String estado, int pagina, int tamano) throws BusinessLogicException {
        try {
            Boolean activo = null;
            if ("ACTIVO".equals(estado)) {
                activo = true;
            }
            if ("INACTIVO".equals(estado)) {
                activo = false;
            }
            int total = libroDAO.contarAdmin(texto, activo);
            List<Libro> libros = libroDAO.listarPaginaAdmin(texto, activo, pagina, tamano, LocalDate.now());
            return new ResultadoPagina<>(total, libros);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public ConteoLibros contar() throws BusinessLogicException {
        try {
            return libroDAO.contarTodos();
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public Libro insertar(Libro libro) throws BusinessLogicException {
        try {
            libro.setFechaRegistro(LocalDateTime.now().withNano(0));
            libro.setId(libroDAO.insertar(libro));
            return libro;
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    @Override
    public void modificar(Libro libro) throws BusinessLogicException {
        try {
            libroDAO.modificar(libro);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RN03: no se borra, se desactiva
    @Override
    public void cambiarActivo(int id, boolean activo) throws BusinessLogicException {
        try {
            libroDAO.cambiarActivo(id, activo);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // Vista de resultados (ERS §2.3 y RF08), común a todas las listas de libros:
    // opciones de los filtros (del conjunto completo), búsqueda, filtros, período por fecha de registro,
    // orden (si la vista lo permite) y la página pedida. El conjunto llega ya en el orden propio de la vista.
    private ResultadoLibros filtrar(List<Libro> conjunto, FiltroLibros filtro, boolean permiteOrden) {
        Map<Integer, Entidad> categorias = new LinkedHashMap<>();
        Map<Integer, Entidad> autores = new LinkedHashMap<>();
        Map<Integer, Entidad> editoriales = new LinkedHashMap<>();
        List<String> idiomas = new ArrayList<>();
        for (Libro libro : conjunto) {
            categorias.put(libro.getCategoria().getId(), libro.getCategoria());
            autores.put(libro.getAutor().getId(), libro.getAutor());
            editoriales.put(libro.getEditorial().getId(), libro.getEditorial());
            if (!idiomas.contains(libro.getIdioma())) {
                idiomas.add(libro.getIdioma());
            }
        }

        // Dentro de un filtro basta cumplir un valor (o); entre filtros hay que cumplir todos (y)
        List<Libro> libros = new ArrayList<>();
        for (Libro libro : conjunto) {
            if (filtro.q() != null && !libro.getTitulo().toLowerCase().contains(filtro.q().toLowerCase())) continue;
            if (!filtro.categorias().isEmpty() && !filtro.categorias().contains(libro.getCategoria().getId())) continue;
            if (!filtro.autores().isEmpty() && !filtro.autores().contains(libro.getAutor().getId())) continue;
            if (!filtro.editoriales().isEmpty() && !filtro.editoriales().contains(libro.getEditorial().getId())) continue;
            if (filtro.idioma() != null && !filtro.idioma().equals(libro.getIdioma())) continue;
            if (filtro.min() != null && libro.getPrecioFinal() < filtro.min()) continue;
            if (filtro.max() != null && libro.getPrecioFinal() > filtro.max()) continue;
            LocalDate registro = libro.getFechaRegistro().toLocalDate();
            if (filtro.desde() != null && registro.isBefore(filtro.desde())) continue;
            if (filtro.hasta() != null && registro.isAfter(filtro.hasta())) continue;
            libros.add(libro);
        }

        if (permiteOrden) {
            String orden = filtro.orden();
            if ("titulo-desc".equals(orden)) {
                libros.sort((a, b) -> alfabetico.compare(b.getTitulo(), a.getTitulo()));
            } else if ("precio-asc".equals(orden)) {
                libros.sort((a, b) -> Double.compare(a.getPrecioFinal(), b.getPrecioFinal()));
            } else if ("precio-desc".equals(orden)) {
                libros.sort((a, b) -> Double.compare(b.getPrecioFinal(), a.getPrecioFinal()));
            } else {
                libros.sort((a, b) -> alfabetico.compare(a.getTitulo(), b.getTitulo()));
            }
        }

        int total = libros.size();
        int totalPaginas = Math.max(1, (total + filtro.tamano() - 1) / filtro.tamano());
        int desde = Math.min((filtro.pagina() - 1) * filtro.tamano(), total);
        int hasta = Math.min(desde + filtro.tamano(), total);

        List<Entidad> listaCategorias = new ArrayList<>(categorias.values());
        List<Entidad> listaAutores = new ArrayList<>(autores.values());
        List<Entidad> listaEditoriales = new ArrayList<>(editoriales.values());
        listaCategorias.sort((a, b) -> alfabetico.compare(a.getNombre(), b.getNombre()));
        listaAutores.sort((a, b) -> alfabetico.compare(a.getNombre(), b.getNombre()));
        listaEditoriales.sort((a, b) -> alfabetico.compare(a.getNombre(), b.getNombre()));
        idiomas.sort(alfabetico);

        return new ResultadoLibros(total, filtro.pagina(), totalPaginas, new ArrayList<>(libros.subList(desde, hasta)),
                listaCategorias, listaAutores, listaEditoriales, idiomas);
    }
}
