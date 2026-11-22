package pe.edu.pucp.bookshop.bl.impl;

import pe.edu.pucp.bookshop.bl.LibroBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.NoEncontradoException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.LibroDAO;
import pe.edu.pucp.bookshop.dao.ListaLibroDAO;
import pe.edu.pucp.bookshop.dao.PedidoDAO;
import pe.edu.pucp.bookshop.dao.ResenaDAO;
import pe.edu.pucp.bookshop.dao.impl.LibroDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.ListaLibroDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.PedidoDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.ResenaDAOImpl;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.EstadoPedido;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.Resena;
import pe.edu.pucp.bookshop.model.TipoLista;
import pe.edu.pucp.bookshop.model.dto.ConteoLibros;
import pe.edu.pucp.bookshop.model.dto.FiltroLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoLibros;
import pe.edu.pucp.bookshop.model.dto.ResultadoPagina;
import pe.edu.pucp.bookshop.model.dto.Venta;

import java.sql.SQLException;
import java.text.Collator;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LibroBLImpl implements LibroBL {

    private LibroDAO libroDAO = new LibroDAOImpl();
    private ListaLibroDAO listaDAO = new ListaLibroDAOImpl();
    private PedidoDAO pedidoDAO = new PedidoDAOImpl();
    private ResenaDAO resenaDAO = new ResenaDAOImpl();
    // Orden alfabético en español (tildes y ñ)
    private Collator alfabetico = Collator.getInstance(Locale.of("es"));

    // RF07.6: el catálogo es la vista de resultados de todos los libros activos
    @Override
    public ResultadoLibros listarCatalogo(FiltroLibros filtro) throws BusinessLogicException {
        try {
            List<Libro> activos = libroDAO.listarActivos(LocalDate.now());
            return filtrar(activos, filtro, true, true);
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
            return filtrar(activos, filtro, false, true);
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
            return filtrar(libros, filtro, true, true);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF06.3: los libros activos de una lista del cliente, como vista de resultados con orden
    @Override
    public ResultadoLibros listarLista(int idUsuario, TipoLista lista, FiltroLibros filtro) throws BusinessLogicException {
        try {
            List<Integer> enLista = listaDAO.listarLibros(idUsuario, lista);
            List<Libro> libros = new ArrayList<>();
            for (Libro libro : libroDAO.listarActivos(LocalDate.now())) {
                if (enLista.contains(libro.getId())) {
                    libros.add(libro);
                }
            }
            return filtrar(libros, filtro, true, true);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF14: puntaje = unidades vendidas (ventas válidas) en el período
    @Override
    public ResultadoLibros listarMasVendidos(FiltroLibros filtro) throws BusinessLogicException {
        try {
            Map<Integer, Integer> unidades = new HashMap<>();
            for (Venta venta : pedidoDAO.listarVentas(EstadoPedido.VENTA_VALIDA, filtro.desde(), filtro.hasta())) {
                int id = venta.libro().getId();
                unidades.put(id, unidades.getOrDefault(id, 0) + venta.cantidad());
            }
            List<Libro> libros = new ArrayList<>();
            for (Libro libro : libroDAO.listarActivos(LocalDate.now())) {
                if (unidades.containsKey(libro.getId())) {
                    libros.add(libro);
                }
            }
            libros.sort((a, b) -> unidades.get(b.getId()) - unidades.get(a.getId()));
            return filtrar(libros, filtro, false, false);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF13.4–5: para cada libro con al menos una venta válida o una reseña en el período,
    // puntaje = 0.5 × (V / Vmáx) + 0.3 × (B / 5) + 0.2 × R
    @Override
    public ResultadoLibros listarMasPopulares(FiltroLibros filtro) throws BusinessLogicException {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            // Ventas con decaimiento: V = Σ cantidad × 0.5^(díasDesdeLaVenta / 30)
            Map<Integer, Double> ventas = new HashMap<>();
            double ventasMaximo = 0;
            for (Venta venta : pedidoDAO.listarVentas(EstadoPedido.VENTA_VALIDA, filtro.desde(), filtro.hasta())) {
                int id = venta.libro().getId();
                double dias = Duration.between(venta.fecha(), ahora).toMinutes() / 1440.0;
                ventas.put(id, ventas.getOrDefault(id, 0.0) + venta.cantidad() * Math.pow(0.5, dias / 30));
                ventasMaximo = Math.max(ventasMaximo, ventas.get(id));
            }
            // Reseñas visibles del período: suma de puntuaciones y cantidad por libro, y m = promedio del sistema
            Map<Integer, Integer> sumaPuntos = new HashMap<>();
            Map<Integer, Integer> cantidadResenas = new HashMap<>();
            int sumaSistema = 0;
            int resenasSistema = 0;
            for (Resena resena : resenaDAO.listarActivas()) {
                LocalDate dia = resena.getFecha().toLocalDate();
                if (filtro.desde() != null && dia.isBefore(filtro.desde())) continue;
                if (filtro.hasta() != null && dia.isAfter(filtro.hasta())) continue;
                int id = resena.getLibro().getId();
                sumaPuntos.put(id, sumaPuntos.getOrDefault(id, 0) + resena.getPuntuacion());
                cantidadResenas.put(id, cantidadResenas.getOrDefault(id, 0) + 1);
                sumaSistema += resena.getPuntuacion();
                resenasSistema++;
            }
            double m = resenasSistema > 0 ? (double) sumaSistema / resenasSistema : 0;

            Map<Integer, Double> puntaje = new HashMap<>();
            List<Libro> libros = new ArrayList<>();
            for (Libro libro : libroDAO.listarActivos(LocalDate.now())) {
                int id = libro.getId();
                if (!ventas.containsKey(id) && !cantidadResenas.containsKey(id)) continue;
                double v = ventasMaximo > 0 ? ventas.getOrDefault(id, 0.0) / ventasMaximo : 0;
                // Valoración bayesiana: B = (5 × m + suma de puntuaciones) / (5 + n)
                double b = (5 * m + sumaPuntos.getOrDefault(id, 0)) / (5 + cantidadResenas.getOrDefault(id, 0));
                // Recencia: R = 0.5^(díasDesdeElRegistroDelLibro / 90)
                double dias = Duration.between(libro.getFechaRegistro(), ahora).toMinutes() / 1440.0;
                double r = Math.pow(0.5, dias / 90);
                puntaje.put(id, 0.5 * v + 0.3 * (b / 5) + 0.2 * r);
                libros.add(libro);
            }
            libros.sort((a, b) -> Double.compare(puntaje.get(b.getId()), puntaje.get(a.getId())));
            return filtrar(libros, filtro, false, false);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF13.1–3: filtrado colaborativo. Jaccard = |A ∩ B| / |A ∪ B| con cada otro cliente; se toman los 10 con
    // mayor Jaccard (> 0) y cada libro que compraron y el cliente no tiene suma los Jaccard de quienes lo compraron.
    // Solo con historial suficiente: ventas válidas de al menos 3 libros distintos (ERS §3.2).
    @Override
    public ResultadoLibros listarParaTi(int idUsuario, FiltroLibros filtro) throws BusinessLogicException {
        try {
            Map<Integer, Set<Integer>> compras = new HashMap<>();
            for (Venta venta : pedidoDAO.listarVentas(EstadoPedido.VENTA_VALIDA, filtro.desde(), filtro.hasta())) {
                if (!compras.containsKey(venta.idUsuario())) {
                    compras.put(venta.idUsuario(), new HashSet<>());
                }
                compras.get(venta.idUsuario()).add(venta.libro().getId());
            }
            Set<Integer> mios = compras.getOrDefault(idUsuario, new HashSet<>());
            if (mios.size() < 3) {
                return filtrar(new ArrayList<>(), filtro, false, false);
            }

            Map<Integer, Double> jaccard = new HashMap<>();
            for (int otro : compras.keySet()) {
                if (otro == idUsuario) continue;
                int interseccion = 0;
                for (int libro : compras.get(otro)) {
                    if (mios.contains(libro)) interseccion++;
                }
                int union = mios.size() + compras.get(otro).size() - interseccion;
                if (interseccion > 0) {
                    jaccard.put(otro, (double) interseccion / union);
                }
            }
            List<Integer> vecinos = new ArrayList<>(jaccard.keySet());
            vecinos.sort((a, b) -> Double.compare(jaccard.get(b), jaccard.get(a)));
            if (vecinos.size() > 10) {
                vecinos = vecinos.subList(0, 10);
            }

            Map<Integer, Double> puntaje = new HashMap<>();
            for (int vecino : vecinos) {
                for (int libro : compras.get(vecino)) {
                    if (!mios.contains(libro)) {
                        puntaje.put(libro, puntaje.getOrDefault(libro, 0.0) + jaccard.get(vecino));
                    }
                }
            }
            List<Libro> libros = new ArrayList<>();
            for (Libro libro : libroDAO.listarActivos(LocalDate.now())) {
                if (puntaje.containsKey(libro.getId())) {
                    libros.add(libro);
                }
            }
            libros.sort((a, b) -> Double.compare(puntaje.get(b.getId()), puntaje.get(a.getId())));
            return filtrar(libros, filtro, false, false);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF12.1–3: similitud del coseno entre vectores de características. Categoría, autor, editorial e idioma
    // en one-hot (aportan 1 al producto si coinciden) y precio, páginas y año normalizados entre 0 y 1
    // sobre los libros activos. El período filtra por fecha de registro.
    @Override
    public ResultadoLibros listarSimilares(int idLibro, FiltroLibros filtro) throws BusinessLogicException {
        try {
            List<Libro> activos = libroDAO.listarActivos(LocalDate.now());
            Libro base = libroDAO.obtener(idLibro, LocalDate.now());
            double precioMin = Double.MAX_VALUE, precioMax = 0, paginasMin = Double.MAX_VALUE, paginasMax = 0;
            double anioMin = Double.MAX_VALUE, anioMax = 0;
            for (Libro libro : activos) {
                precioMin = Math.min(precioMin, libro.getPrecio());
                precioMax = Math.max(precioMax, libro.getPrecio());
                paginasMin = Math.min(paginasMin, libro.getPaginas());
                paginasMax = Math.max(paginasMax, libro.getPaginas());
                anioMin = Math.min(anioMin, libro.getAnioPublicacion());
                anioMax = Math.max(anioMax, libro.getAnioPublicacion());
            }
            // (valor − mínimo) / (máximo − mínimo); si todos son iguales, el rango se deja en 1
            double precioRango = precioMax > precioMin ? precioMax - precioMin : 1;
            double paginasRango = paginasMax > paginasMin ? paginasMax - paginasMin : 1;
            double anioRango = anioMax > anioMin ? anioMax - anioMin : 1;
            double precioL = (base.getPrecio() - precioMin) / precioRango;
            double paginasL = (base.getPaginas() - paginasMin) / paginasRango;
            double anioL = (base.getAnioPublicacion() - anioMin) / anioRango;
            double normaL = Math.sqrt(4 + precioL * precioL + paginasL * paginasL + anioL * anioL);

            Map<Integer, Double> similitud = new HashMap<>();
            List<Libro> libros = new ArrayList<>();
            for (Libro libro : activos) {
                if (libro.getId() == idLibro) continue;
                double precioB = (libro.getPrecio() - precioMin) / precioRango;
                double paginasB = (libro.getPaginas() - paginasMin) / paginasRango;
                double anioB = (libro.getAnioPublicacion() - anioMin) / anioRango;
                double producto = precioL * precioB + paginasL * paginasB + anioL * anioB;
                if (libro.getCategoria().getId().equals(base.getCategoria().getId())) producto++;
                if (libro.getAutor().getId().equals(base.getAutor().getId())) producto++;
                if (libro.getEditorial().getId().equals(base.getEditorial().getId())) producto++;
                if (libro.getIdioma().equals(base.getIdioma())) producto++;
                double normaB = Math.sqrt(4 + precioB * precioB + paginasB * paginasB + anioB * anioB);
                // cos(L, B) = (L · B) / (|L| × |B|)
                similitud.put(libro.getId(), producto / (normaL * normaB));
                libros.add(libro);
            }
            libros.sort((a, b) -> Double.compare(similitud.get(b.getId()), similitud.get(a.getId())));
            return filtrar(libros, filtro, false, true);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF12.4–6: reglas de asociación sobre los pedidos válidos del período. Para X del carrito e Y fuera de él:
    // confianza(X→Y) = pedidos con X e Y / pedidos con X; lift(X→Y) = confianza / (pedidos con Y / total de pedidos).
    // Para cada Y vale la regla de mayor confianza; se muestran los Y con lift > 1, por confianza y luego por lift.
    @Override
    public ResultadoLibros listarComplementarios(List<Integer> carrito, FiltroLibros filtro) throws BusinessLogicException {
        try {
            Map<Integer, Set<Integer>> pedidos = new HashMap<>();
            for (Venta venta : pedidoDAO.listarVentas(EstadoPedido.VENTA_VALIDA, filtro.desde(), filtro.hasta())) {
                if (!pedidos.containsKey(venta.idPedido())) {
                    pedidos.put(venta.idPedido(), new HashSet<>());
                }
                pedidos.get(venta.idPedido()).add(venta.libro().getId());
            }
            Map<Integer, Integer> pedidosConLibro = new HashMap<>();
            for (Set<Integer> libros : pedidos.values()) {
                for (int libro : libros) {
                    pedidosConLibro.put(libro, pedidosConLibro.getOrDefault(libro, 0) + 1);
                }
            }

            Map<Integer, Double> confianza = new HashMap<>();
            Map<Integer, Double> lift = new HashMap<>();
            for (int x : carrito) {
                int conX = pedidosConLibro.getOrDefault(x, 0);
                if (conX == 0) continue;
                Map<Integer, Integer> conXeY = new HashMap<>();
                for (Set<Integer> libros : pedidos.values()) {
                    if (!libros.contains(x)) continue;
                    for (int y : libros) {
                        if (!carrito.contains(y)) {
                            conXeY.put(y, conXeY.getOrDefault(y, 0) + 1);
                        }
                    }
                }
                for (int y : conXeY.keySet()) {
                    double conf = (double) conXeY.get(y) / conX;
                    if (conf > confianza.getOrDefault(y, 0.0)) {
                        confianza.put(y, conf);
                        lift.put(y, conf / ((double) pedidosConLibro.get(y) / pedidos.size()));
                    }
                }
            }
            List<Libro> libros = new ArrayList<>();
            for (Libro libro : libroDAO.listarActivos(LocalDate.now())) {
                if (lift.containsKey(libro.getId()) && lift.get(libro.getId()) > 1) {
                    libros.add(libro);
                }
            }
            libros.sort((a, b) -> {
                int porConfianza = Double.compare(confianza.get(b.getId()), confianza.get(a.getId()));
                return porConfianza != 0 ? porConfianza : Double.compare(lift.get(b.getId()), lift.get(a.getId()));
            });
            return filtrar(libros, filtro, false, false);
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
    // opciones de los filtros (del conjunto completo), búsqueda, filtros, período por fecha de registro
    // (si periodoPorRegistro; en las vistas calculadas con ventas el período ya delimitó las ventas),
    // orden (si la vista lo permite) y la página pedida. El conjunto llega ya en el orden propio de la vista.
    private ResultadoLibros filtrar(List<Libro> conjunto, FiltroLibros filtro, boolean permiteOrden, boolean periodoPorRegistro) {
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
            if (periodoPorRegistro && filtro.desde() != null && registro.isBefore(filtro.desde())) continue;
            if (periodoPorRegistro && filtro.hasta() != null && registro.isAfter(filtro.hasta())) continue;
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
