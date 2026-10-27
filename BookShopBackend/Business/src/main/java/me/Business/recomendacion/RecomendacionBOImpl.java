package me.Business.recomendacion;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.Business.Validaciones;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.catalogo.ResenaDAO;
import me.DAO.catalogo.ResenaDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DAO.venta.PedidoDAO;
import me.DAO.venta.PedidoDAOImpl;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.ConsultaRecomendacionesDTO;
import me.Model.dto.catalogo.OrdenLibro;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.dto.compra.VentaLibroDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;
import me.Model.entities.catalogo.Libro;

public class RecomendacionBOImpl implements RecomendacionBO {
    private final LibroDAO libros;
    private final PedidoDAO pedidos;
    private final ResenaDAO resenas;
    private final Autorizacion autorizacion;
    private final Clock reloj;
    private final Map<String, Conjunto> consultas = new ConcurrentHashMap<>();

    public RecomendacionBOImpl() {
        this(new LibroDAOImpl(), new PedidoDAOImpl(), new ResenaDAOImpl(), new UsuarioDAOImpl(),
                Clock.system(ZoneId.of("America/Lima")));
    }
    public RecomendacionBOImpl(LibroDAO libros, PedidoDAO pedidos, ResenaDAO resenas,
            UsuarioDAO usuarios, Clock reloj) {
        this.libros = libros;
        this.pedidos = pedidos;
        this.resenas = resenas;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public List<TarjetaLibroDTO> similares(int idLibro) throws NegocioException {
        if (idLibro < 1) throw new ValidacionException("idLibro", "Selecciona un libro.");
        try {
            Datos datos = cargar();
            Libro origen = datos.libros.stream().filter(l -> l.getId() == idLibro).findFirst().orElse(null);
            if (origen == null) throw new RecursoNoEncontradoException("El libro no está disponible.");
            Map<Integer, Double> puntos = SimilitudContenido.puntuar(origen, datos.libros);
            List<Integer> ids = ordenarPuntos(datos.libros, puntos, Set.of(idLibro));
            return libros.listarTarjetasPorIds(ids.subList(0, Math.min(8, ids.size())), datos.ahora);
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los libros similares.", e); }
    }

    @Override
    public List<TarjetaLibroDTO> complementarios(List<Integer> idsCarrito) throws NegocioException {
        if (idsCarrito == null || idsCarrito.stream().anyMatch(id -> id == null || id < 1))
            throw new ValidacionException("carrito", "Los libros del carrito no son válidos.");
        if (idsCarrito.isEmpty()) return List.of();
        Set<Integer> carrito = new HashSet<>(idsCarrito);
        try {
            Datos datos = cargar();
            Map<Integer, Double> puntos = AsociacionCompras.puntuar(carrito, datos.ventas);
            List<Integer> ids = conRespaldo(datos, puntos, carrito);
            return libros.listarTarjetasPorIds(ids.subList(0, Math.min(8, ids.size())), datos.ahora);
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los libros complementarios.", e); }
    }

    @Override
    public boolean puedeMostrarParaTi(int idActor) throws NegocioException {
        if (idActor == 0) return false;
        autorizacion.usuarioActivo(idActor);
        try {
            return FiltradoColaborativo.pedidosUsuario(idActor, ventasValidas()).size() >= 3;
        } catch (SQLException e) { throw new PersistenciaException("No se pudo comprobar el historial de recomendaciones.", e); }
    }

    @Override
    public RecomendacionesDTO abrirPopulares(ConsultaRecomendacionesDTO consulta) throws NegocioException {
        validarConsulta(consulta);
        try {
            Datos datos = cargar();
            Map<Integer, Double> puntos = Popularidad.puntuar(datos.libros, datos.ventas, datos.valoraciones, datos.ahora);
            return abrir(0, ordenarPuntos(datos.libros, puntos, Set.of()), consulta);
        } catch (SQLException e) { throw new PersistenciaException("No se pudo consultar el ranking de popularidad.", e); }
    }

    @Override
    public RecomendacionesDTO abrirParaTi(int idActor, ConsultaRecomendacionesDTO consulta) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        validarConsulta(consulta);
        try {
            Datos datos = cargar();
            if (FiltradoColaborativo.pedidosUsuario(idActor, datos.ventas).size() < 3)
                throw new ConflictoException("Para ti requiere al menos tres pedidos confirmados válidos.");
            Set<Integer> comprados = FiltradoColaborativo.librosUsuario(idActor, datos.ventas);
            return abrir(idActor, conRespaldo(datos,
                    FiltradoColaborativo.puntuar(idActor, datos.ventas), comprados), consulta);
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron preparar tus recomendaciones.", e); }
    }

    private RecomendacionesDTO abrir(int propietario, List<Integer> ids,
            ConsultaRecomendacionesDTO consulta) throws NegocioException, SQLException {
        Conjunto conjunto = new Conjunto(propietario, List.copyOf(ids));
        String id = UUID.randomUUID().toString();
        ResultadoPaginadoDTO<TarjetaLibroDTO> pagina = pagina(conjunto, consulta);
        consultas.put(id, conjunto);
        return new RecomendacionesDTO(id, pagina);
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> consultar(int idActor, String idConsulta,
            ConsultaRecomendacionesDTO consulta) throws NegocioException {
        validarConsulta(consulta);
        Conjunto conjunto = obtener(idActor, idConsulta);
        try { return pagina(conjunto, consulta); }
        catch (SQLException e) { throw new PersistenciaException("No se pudo consultar la página de recomendaciones.", e); }
    }

    @Override
    public void descartar(int idActor, String idConsulta) throws NegocioException {
        obtener(idActor, idConsulta);
        consultas.remove(idConsulta);
    }

    private Conjunto obtener(int idActor, String idConsulta) throws NegocioException {
        if (idConsulta == null || idConsulta.isBlank())
            throw new ValidacionException("idConsulta", "Falta la consulta de recomendaciones.");
        Conjunto conjunto = consultas.get(idConsulta);
        if (conjunto == null) throw new RecursoNoEncontradoException("Vuelve a abrir las recomendaciones.");
        if (conjunto.propietario != 0) autorizacion.propietario(idActor, conjunto.propietario);
        return conjunto;
    }

    private ResultadoPaginadoDTO<TarjetaLibroDTO> pagina(Conjunto conjunto,
            ConsultaRecomendacionesDTO consulta) throws SQLException {
        LocalDateTime fecha = LocalDateTime.now(reloj);
        Map<Integer, TarjetaLibroDTO> precios = new HashMap<>();
        for (var tarjeta : libros.listarTarjetasSinImagenPorIds(conjunto.ids, fecha)) precios.put(tarjeta.id(), tarjeta);
        Map<Integer, Libro> atributos = new HashMap<>();
        for (var libro : libros.listarParaRecomendaciones()) atributos.put(libro.getId(), libro);
        // Refresca atributos del conjunto original; no repite el algoritmo ni incorpora otros libros.
        List<Integer> ids = new ArrayList<>(conjunto.ids.stream()
                .filter(id -> precios.containsKey(id) && atributos.containsKey(id)).toList());
        Comparator<Integer> orden = switch (consulta.orden()) {
            case MAS_RECIENTES -> Comparator.comparing((Integer id) -> atributos.get(id).getFechaCreacion()).reversed();
            case PRECIO_ASCENDENTE -> Comparator.comparing((Integer id) -> precios.get(id).precioConDescuento());
            case PRECIO_DESCENDENTE -> Comparator.comparing((Integer id) -> precios.get(id).precioConDescuento()).reversed();
            case TITULO_ASCENDENTE -> Comparator.comparing((Integer id) -> precios.get(id).titulo(), String.CASE_INSENSITIVE_ORDER);
            case PAGINAS_ASCENDENTE -> Comparator.comparingInt((Integer id) -> atributos.get(id).getNumeroPaginas());
            case PAGINAS_DESCENDENTE -> Comparator.comparingInt((Integer id) -> atributos.get(id).getNumeroPaginas()).reversed();
            case STOCK_DESCENDENTE -> Comparator.comparingInt((Integer id) -> atributos.get(id).getStockFisico()).reversed();
            case ANIO_EDICION_DESCENDENTE -> Comparator.comparingInt((Integer id) -> atributos.get(id).getAnioEdicion()).reversed();
            default -> null;
        };
        // List.sort es estable: los empates conservan el orden del algoritmo sobre todo el conjunto.
        if (orden != null) ids.sort(orden);
        int inicio = (int) Math.min(ids.size(), (long) (consulta.pagina() - 1) * consulta.tamanioPagina());
        int fin = (int) Math.min(ids.size(), (long) inicio + consulta.tamanioPagina());
        List<TarjetaLibroDTO> tarjetas = libros.listarTarjetasPorIds(ids.subList(inicio, fin), fecha);
        return new ResultadoPaginadoDTO<>(tarjetas, ids.size(), consulta.pagina(), consulta.tamanioPagina());
    }

    private List<Integer> conRespaldo(Datos datos, Map<Integer, Double> puntos, Set<Integer> excluir) {
        List<Integer> ids = ordenarPuntos(datos.libros, puntos, excluir);
        Map<Integer, Double> populares = Popularidad.puntuar(datos.libros, datos.ventas, datos.valoraciones, datos.ahora);
        Set<Integer> yaIncluidos = new HashSet<>(excluir);
        yaIncluidos.addAll(ids);
        ids.addAll(ordenarPuntos(datos.libros, populares, yaIncluidos));
        return ids;
    }

    private List<Integer> ordenarPuntos(List<Libro> libros, Map<Integer, Double> puntos, Set<Integer> excluir) {
        List<Integer> ids = new ArrayList<>();
        for (Libro libro : libros)
            if (!excluir.contains(libro.getId()) && puntos.containsKey(libro.getId())) ids.add(libro.getId());
        ids.sort(Comparator.comparingDouble((Integer id) -> puntos.get(id)).reversed().thenComparingInt(Integer::intValue));
        return ids;
    }

    private Datos cargar() throws SQLException {
        return new Datos(libros.listarParaRecomendaciones(), ventasValidas(), resenas.listarValoraciones(), LocalDateTime.now(reloj));
    }

    private List<VentaLibroDTO> ventasValidas() throws SQLException {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        // DAO excluye cancelados/devueltos. El conjunto completo conserva la elegibilidad de Para ti.
        return pedidos.listarDetalleVentas(LocalDate.of(1000, 1, 1)).stream()
                .filter(v -> !v.fechaPago().isAfter(ahora)).toList();
    }

    private void validarConsulta(ConsultaRecomendacionesDTO consulta) throws ValidacionException {
        if (consulta == null || consulta.orden() == null)
            throw new ValidacionException("consulta", "Selecciona un orden de recomendaciones.");
        if (consulta.orden() == OrdenLibro.MAS_VENDIDOS)
            throw new ValidacionException("orden", "Más vendidos corresponde al catálogo.");
        Validaciones.pagina(consulta.pagina(), consulta.tamanioPagina());
    }

    private record Datos(List<Libro> libros, List<VentaLibroDTO> ventas,
            List<ValoracionLibroDTO> valoraciones, LocalDateTime ahora) { }
    private record Conjunto(int propietario, List<Integer> ids) { }
}
