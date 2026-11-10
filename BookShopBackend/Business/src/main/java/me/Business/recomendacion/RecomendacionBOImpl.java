package me.Business.recomendacion;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
    private final LibroDAO libroDAO;
    private final PedidoDAO pedidoDAO;
    private final ResenaDAO resenaDAO;
    private final Autorizacion autorizacion;
    private final Clock reloj;
    private final Map<String, ConsultaGuardada> consultas = new ConcurrentHashMap<>();

    public RecomendacionBOImpl() {
        this(new LibroDAOImpl(), new PedidoDAOImpl(), new ResenaDAOImpl(), new UsuarioDAOImpl(),
            Clock.system(ZoneId.of("America/Lima")));
    }
    public RecomendacionBOImpl(LibroDAO libroDAO, PedidoDAO pedidoDAO, ResenaDAO resenaDAO,
        UsuarioDAO usuarioDAO, Clock reloj) {
        this.libroDAO = libroDAO;
        this.pedidoDAO = pedidoDAO;
        this.resenaDAO = resenaDAO;
        this.autorizacion = new Autorizacion(usuarioDAO);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public List<TarjetaLibroDTO> similares(int idLibro) throws NegocioException {
        if (idLibro < 1) throw new ValidacionException("idLibro", "Selecciona un libro.");
        try {
            DatosRecomendacion datos = cargarDatos();
            Libro origen = null;
            for (Libro candidato : datos.libros) {
                if (candidato.getId() == idLibro) {
                    origen = candidato;
                    break;
                }
            }
            if (origen == null) throw new RecursoNoEncontradoException("El libro no está disponible.");
            Map<Integer, Double> puntos = SimilitudContenido.puntuar(origen, datos.libros);
            List<Integer> ids = ordenarPorPuntuacion(datos.libros, puntos, Set.of(idLibro));
            return libroDAO.listarTarjetasPorIds(ids.subList(0, Math.min(8, ids.size())), datos.ahora);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros similares.", e);
        }
    }

    @Override
    public List<TarjetaLibroDTO> complementarios(List<Integer> idsCarrito) throws NegocioException {
        if (idsCarrito == null) throw new ValidacionException("carrito", "Indica los libros del carrito.");
        for (Integer id : idsCarrito) {
            if (id == null || id < 1) throw new ValidacionException("carrito", "Los libros del carrito no son validos.");
        }
        if (idsCarrito.isEmpty()) return List.of();
        Set<Integer> carrito = new HashSet<>(idsCarrito);
        try {
            DatosRecomendacion datos = cargarDatos();
            Map<Integer, Double> puntos = AsociacionCompras.puntuar(carrito, datos.ventas);
            List<Integer> ids = completarConPopulares(datos, puntos, carrito);
            return libroDAO.listarTarjetasPorIds(ids.subList(0, Math.min(8, ids.size())), datos.ahora);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros complementarios.", e);
        }
    }

    @Override
    public boolean puedeMostrarParaTi(int idActor) throws NegocioException {
        if (idActor == 0) return false;
        autorizacion.usuarioActivo(idActor);
        try {
            return FiltradoColaborativo.pedidosUsuario(idActor, ventasValidas()).size() >= 3;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar el historial de recomendaciones.", e);
        }
    }

    @Override
    public RecomendacionesDTO abrirPopulares(ConsultaRecomendacionesDTO consulta) throws NegocioException {
        validarConsulta(consulta);
        try {
            DatosRecomendacion datos = cargarDatos();
            Map<Integer, Double> puntos = Popularidad.puntuar(datos.libros, datos.ventas, datos.valoraciones, datos.ahora);
            return abrir(0, ordenarPorPuntuacion(datos.libros, puntos, Set.of()), consulta);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el ranking de popularidad.", e);
        }
    }

    @Override
    public RecomendacionesDTO abrirParaTi(int idActor, ConsultaRecomendacionesDTO consulta) throws NegocioException {
        autorizacion.usuarioActivo(idActor);
        validarConsulta(consulta);
        try {
            DatosRecomendacion datos = cargarDatos();
            if (FiltradoColaborativo.pedidosUsuario(idActor, datos.ventas).size() < 3)
                throw new ConflictoException("Para ti requiere al menos tres pedidos confirmados válidos.");
            Set<Integer> comprados = FiltradoColaborativo.librosUsuario(idActor, datos.ventas);
            return abrir(idActor, completarConPopulares(datos,
                FiltradoColaborativo.puntuar(idActor, datos.ventas), comprados), consulta);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron preparar tus recomendaciones.", e);
        }
    }

    private RecomendacionesDTO abrir(int propietario, List<Integer> ids,
        ConsultaRecomendacionesDTO consulta) throws NegocioException, SQLException {
        ConsultaGuardada conjunto = new ConsultaGuardada(propietario, List.copyOf(ids));
        String id = UUID.randomUUID().toString();
        ResultadoPaginadoDTO<TarjetaLibroDTO> pagina = consultarPagina(conjunto, consulta);
        consultas.put(id, conjunto);
        return new RecomendacionesDTO(id, pagina);
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> consultar(int idActor, String idConsulta,
        ConsultaRecomendacionesDTO consulta) throws NegocioException {
        validarConsulta(consulta);
        ConsultaGuardada conjunto = obtener(idActor, idConsulta);
        try {
            return consultarPagina(conjunto, consulta);
        }
        catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la página de recomendaciones.", e);
        }
    }

    @Override
    public void descartar(int idActor, String idConsulta) throws NegocioException {
        obtener(idActor, idConsulta);
        consultas.remove(idConsulta);
    }

    private ConsultaGuardada obtener(int idActor, String idConsulta) throws NegocioException {
        if (idConsulta == null || idConsulta.isBlank())
            throw new ValidacionException("idConsulta", "Falta la consulta de recomendaciones.");
        ConsultaGuardada conjunto = consultas.get(idConsulta);
        if (conjunto == null) throw new RecursoNoEncontradoException("Vuelve a abrir las recomendaciones.");
        if (conjunto.propietario != 0) autorizacion.propietario(idActor, conjunto.propietario);
        return conjunto;
    }

    private ResultadoPaginadoDTO<TarjetaLibroDTO> consultarPagina(ConsultaGuardada conjunto,
        ConsultaRecomendacionesDTO consulta) throws SQLException {
        LocalDateTime fecha = LocalDateTime.now(reloj);
        Map<Integer, TarjetaLibroDTO> precios = new HashMap<>();
        for (TarjetaLibroDTO tarjeta : libroDAO.listarTarjetasSinImagenPorIds(conjunto.ids, fecha))
            precios.put(tarjeta.id(), tarjeta);
        Map<Integer, Libro> atributos = new HashMap<>();
        for (Libro libro : libroDAO.listarParaRecomendaciones()) atributos.put(libro.getId(), libro);
        // Refresca atributos del conjunto original; no repite el algoritmo ni incorpora otros libros.
        List<Integer> ids = new ArrayList<>();
        for (Integer id : conjunto.ids) {
            if (precios.containsKey(id) && atributos.containsKey(id)) ids.add(id);
        }
        // sort conserva el orden anterior cuando dos libros empatan.
        ids.sort((primero, segundo) -> {
            Libro libroPrimero = atributos.get(primero);
            Libro libroSegundo = atributos.get(segundo);
            switch (consulta.orden()) {
                case MAS_RECIENTES:
                    return libroSegundo.getFechaCreacion().compareTo(libroPrimero.getFechaCreacion());
                case PRECIO_ASCENDENTE:
                    return precios.get(primero).precioConDescuento().compareTo(precios.get(segundo).precioConDescuento());
                case PRECIO_DESCENDENTE:
                    return precios.get(segundo).precioConDescuento().compareTo(precios.get(primero).precioConDescuento());
                case TITULO_ASCENDENTE:
                    return precios.get(primero).titulo().compareToIgnoreCase(precios.get(segundo).titulo());
                case PAGINAS_ASCENDENTE:
                    return Integer.compare(libroPrimero.getNumeroPaginas(), libroSegundo.getNumeroPaginas());
                case PAGINAS_DESCENDENTE:
                    return Integer.compare(libroSegundo.getNumeroPaginas(), libroPrimero.getNumeroPaginas());
                case STOCK_DESCENDENTE:
                    return Integer.compare(libroSegundo.getStockFisico(), libroPrimero.getStockFisico());
                case ANIO_EDICION_DESCENDENTE:
                    return Integer.compare(libroSegundo.getAnioEdicion(), libroPrimero.getAnioEdicion());
                default:
                    return 0;
            }
        });
        int inicio = (int) Math.min(ids.size(), (long) (consulta.pagina() - 1) * consulta.tamanioPagina());
        int fin = (int) Math.min(ids.size(), (long) inicio + consulta.tamanioPagina());
        List<TarjetaLibroDTO> tarjetas = libroDAO.listarTarjetasPorIds(ids.subList(inicio, fin), fecha);
        return new ResultadoPaginadoDTO<>(tarjetas, ids.size(), consulta.pagina(), consulta.tamanioPagina());
    }

    private List<Integer> completarConPopulares(DatosRecomendacion datos, Map<Integer, Double> puntos, Set<Integer> excluir) {
        List<Integer> ids = ordenarPorPuntuacion(datos.libros, puntos, excluir);
        Map<Integer, Double> populares = Popularidad.puntuar(datos.libros, datos.ventas, datos.valoraciones, datos.ahora);
        Set<Integer> yaIncluidos = new HashSet<>(excluir);
        yaIncluidos.addAll(ids);
        ids.addAll(ordenarPorPuntuacion(datos.libros, populares, yaIncluidos));
        return ids;
    }

    private List<Integer> ordenarPorPuntuacion(List<Libro> libros, Map<Integer, Double> puntos, Set<Integer> excluir) {
        List<Integer> ids = new ArrayList<>();
        for (Libro libro : libros)
            if (!excluir.contains(libro.getId()) && puntos.containsKey(libro.getId())) ids.add(libro.getId());
        ids.sort((primero, segundo) -> {
            int comparacion = Double.compare(puntos.get(segundo), puntos.get(primero));
            if (comparacion == 0) return Integer.compare(primero, segundo);
            return comparacion;
        });
        return ids;
    }

    private DatosRecomendacion cargarDatos() throws SQLException {
        return new DatosRecomendacion(libroDAO.listarParaRecomendaciones(), ventasValidas(), resenaDAO.listarValoraciones(), LocalDateTime.now(reloj));
    }

    private List<VentaLibroDTO> ventasValidas() throws SQLException {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        // DAO excluye cancelados/devueltos. El conjunto completo conserva la elegibilidad de Para ti.
        List<VentaLibroDTO> ventas = new ArrayList<>();
        for (VentaLibroDTO venta : pedidoDAO.listarDetalleVentas(LocalDate.of(1000, 1, 1))) {
            if (!venta.fechaPago().isAfter(ahora)) ventas.add(venta);
        }
        return ventas;
    }

    private void validarConsulta(ConsultaRecomendacionesDTO consulta) throws ValidacionException {
        if (consulta == null || consulta.orden() == null)
            throw new ValidacionException("consulta", "Selecciona un orden de recomendaciones.");
        if (consulta.orden() == OrdenLibro.MAS_VENDIDOS)
            throw new ValidacionException("orden", "Más vendidos corresponde al catálogo.");
        if (consulta.pagina() < 1 || consulta.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La pagina y su tamaño deben ser positivos.");
    }

    private record DatosRecomendacion(List<Libro> libros, List<VentaLibroDTO> ventas,
        List<ValoracionLibroDTO> valoraciones, LocalDateTime ahora) { }
    private record ConsultaGuardada(int propietario, List<Integer> ids) { }
}
