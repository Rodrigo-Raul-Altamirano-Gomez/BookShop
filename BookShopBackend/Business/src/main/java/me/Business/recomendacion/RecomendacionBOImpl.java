package me.Business.recomendacion;

import me.Model.dto.catalogo.RecomendacionesDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.Business.catalogo.LibroBOImpl;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.catalogo.ResenaDAO;
import me.DAO.catalogo.ResenaDAOImpl;
import me.DAO.venta.PedidoDAO;
import me.DAO.venta.PedidoDAOImpl;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.ConsultaRecomendacionesDTO;
import me.Model.dto.catalogo.OrdenLibro;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.dto.compra.VentaLibroDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;
import me.Model.entities.catalogo.Libro;
import me.Business.exceptions.AccesoDenegadoException;

public class RecomendacionBOImpl implements RecomendacionBO {

    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final PedidoDAO pedidoDAO = new PedidoDAOImpl();
    private final ResenaDAO resenaDAO = new ResenaDAOImpl();
    // Consultas abiertas de todos los usuarios; el mapa es compartido, por eso se usa synchronized.
    private static final Map<String, ConsultaGuardada> consultas = new HashMap<>();

    @Override
    public List<TarjetaLibroDTO> similares(int idLibro) throws NegocioException {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            List<Libro> libros = libroDAO.listarParaRecomendaciones();
            Libro origen = null;
            for (Libro candidato : libros) {
                if (candidato.getId() == idLibro) {
                    origen = candidato;
                    break;
                }
            }
            if (origen == null)
                throw new RecursoNoEncontradoException("El libro no está disponible.");
            Map<Integer, Double> puntos = SimilitudContenido.puntuar(origen, libros);
            List<Integer> ids = ordenarPorPuntuacion(libros, puntos, Set.of(idLibro));
            return libroDAO.listarTarjetasPorIds(ids.subList(0, Math.min(8, ids.size())), ahora);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros similares.", e);
        }
    }

    @Override
    public List<TarjetaLibroDTO> complementarios(List<Integer> idsCarrito) throws NegocioException {
        if (idsCarrito.isEmpty())
            return List.of();
        Set<Integer> carrito = new HashSet<>(idsCarrito);
        try {
            LocalDateTime ahora = LocalDateTime.now();
            List<Libro> libros = libroDAO.listarParaRecomendaciones();
            List<VentaLibroDTO> ventas = pedidoDAO.listarDetalleVentas(LocalDate.of(1000, 1, 1));
            Map<Integer, Double> puntos = AsociacionCompras.puntuar(carrito, ventas);
            List<Integer> ids = ordenarPorPuntuacion(libros, puntos, carrito);
            Map<Integer, Double> populares = Popularidad.puntuar(libros, ventas,
                resenaDAO.listarValoraciones(), ahora);
            Set<Integer> excluidos = new HashSet<>(carrito);
            excluidos.addAll(ids);
            ids.addAll(ordenarPorPuntuacion(libros, populares, excluidos));
            return libroDAO.listarTarjetasPorIds(ids.subList(0, Math.min(8, ids.size())), ahora);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los libros complementarios.", e);
        }
    }

    @Override
    public boolean puedeMostrarParaTi(int idActor) throws NegocioException {
        if (idActor == 0)
            return false;
        try {
            List<VentaLibroDTO> ventas = pedidoDAO.listarDetalleVentas(LocalDate.of(1000, 1, 1));
            return FiltradoColaborativo.pedidosUsuario(idActor, ventas).size() >= 3;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar el historial de recomendaciones.", e);
        }
    }

    @Override
    public RecomendacionesDTO abrirPopulares(ConsultaRecomendacionesDTO consulta) throws NegocioException {
        validarConsulta(consulta);
        try {
            LocalDateTime ahora = LocalDateTime.now();
            List<Libro> libros = libroDAO.listarParaRecomendaciones();
            List<VentaLibroDTO> ventas = pedidoDAO.listarDetalleVentas(LocalDate.of(1000, 1, 1));
            List<ValoracionLibroDTO> valoraciones = resenaDAO.listarValoraciones();
            Map<Integer, Double> puntos = Popularidad.puntuar(libros, ventas, valoraciones, ahora);
            List<Integer> ids = ordenarPorPuntuacion(libros, puntos, Set.of());
            String idConsulta = UUID.randomUUID().toString();
            ConsultaGuardada conjunto = new ConsultaGuardada(0, ids);
            ResultadoPaginadoDTO<TarjetaLibroDTO> pagina = consultarPagina(conjunto, consulta);
            synchronized (consultas) {
                consultas.put(idConsulta, conjunto);
            }
            return new RecomendacionesDTO(idConsulta, pagina);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el ranking de popularidad.", e);
        }
    }

    @Override
    public RecomendacionesDTO abrirParaTi(int idActor, ConsultaRecomendacionesDTO consulta) throws
        NegocioException {
        validarConsulta(consulta);
        try {
            LocalDateTime ahora = LocalDateTime.now();
            List<Libro> libros = libroDAO.listarParaRecomendaciones();
            List<VentaLibroDTO> ventas = pedidoDAO.listarDetalleVentas(LocalDate.of(1000, 1, 1));
            if (FiltradoColaborativo.pedidosUsuario(idActor, ventas).size() < 3)
                throw new NegocioException("Para ti requiere al menos tres pedidos confirmados válidos.");
            Set<Integer> comprados = FiltradoColaborativo.librosUsuario(idActor, ventas);
            Map<Integer, Double> puntos = FiltradoColaborativo.puntuar(idActor, ventas);
            List<Integer> ids = ordenarPorPuntuacion(libros, puntos, comprados);
            Map<Integer, Double> populares = Popularidad.puntuar(libros, ventas,
                resenaDAO.listarValoraciones(), ahora);
            Set<Integer> excluidos = new HashSet<>(comprados);
            excluidos.addAll(ids);
            ids.addAll(ordenarPorPuntuacion(libros, populares, excluidos));
            String idConsulta = UUID.randomUUID().toString();
            ConsultaGuardada conjunto = new ConsultaGuardada(idActor, ids);
            ResultadoPaginadoDTO<TarjetaLibroDTO> pagina = consultarPagina(conjunto, consulta);
            synchronized (consultas) {
                consultas.put(idConsulta, conjunto);
            }
            return new RecomendacionesDTO(idConsulta, pagina);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron preparar tus recomendaciones.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> filtrar(int idActor, String idConsulta,
        FiltroLibroDTO filtro) throws NegocioException {
        ConsultaGuardada conjunto;
        synchronized (consultas) {
            conjunto = consultas.get(idConsulta);
        }
        if (conjunto == null)
            throw new RecursoNoEncontradoException("Vuelve a abrir las recomendaciones.");
        if (conjunto.propietario() != 0 && conjunto.propietario() != idActor)
            throw new AccesoDenegadoException("Las recomendaciones pertenecen a otra cuenta.");
        filtro = LibroBOImpl.validarFiltro(filtro);
        try {
            // El catálogo aplica los criterios; el conjunto guardado conserva su orden.
            FiltroLibroDTO completo = new FiltroLibroDTO(filtro.titulo(), filtro.idsCategorias(),
                filtro.idsAutores(), filtro.idsEditoriales(), filtro.idiomas(), filtro.precioMinimo(),
                filtro.precioMaximo(), true, false, OrdenLibro.MAS_RECIENTES, 1, Integer.MAX_VALUE);
            Map<Integer, TarjetaLibroDTO> disponibles = new HashMap<>();
            for (TarjetaLibroDTO libro : libroDAO.buscar(completo, LocalDateTime.now()).items())
                disponibles.put(libro.id(), libro);
            List<TarjetaLibroDTO> resultados = new ArrayList<>();
            for (int id : conjunto.ids()) {
                if (disponibles.containsKey(id)) resultados.add(disponibles.get(id));
            }
            int inicio = Math.min((filtro.pagina() - 1) * filtro.tamanioPagina(), resultados.size());
            int fin = Math.min(inicio + filtro.tamanioPagina(), resultados.size());
            return new ResultadoPaginadoDTO<>(resultados.subList(inicio, fin), resultados.size(),
                filtro.pagina(), filtro.tamanioPagina());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron filtrar las recomendaciones.", e);
        }
    }

    @Override
    public void descartar(int idActor, String idConsulta) throws NegocioException {
        ConsultaGuardada conjunto;
        synchronized (consultas) {
            conjunto = consultas.get(idConsulta);
        }
        if (conjunto == null)
            throw new RecursoNoEncontradoException("Vuelve a abrir las recomendaciones.");
        if (conjunto.propietario() != 0 && conjunto.propietario() != idActor)
            throw new AccesoDenegadoException("Las recomendaciones pertenecen a otra cuenta.");
        synchronized (consultas) {
            consultas.remove(idConsulta);
        }
    }

    private ResultadoPaginadoDTO<TarjetaLibroDTO> consultarPagina(ConsultaGuardada conjunto,
        ConsultaRecomendacionesDTO consulta) throws SQLException {
        // El DAO devuelve las tarjetas disponibles en el orden de los identificadores recibidos.
        List<TarjetaLibroDTO> tarjetas = libroDAO.listarTarjetasPorIds(conjunto.ids, LocalDateTime.now());
        int inicio = Math.min((consulta.pagina() - 1) * consulta.tamanioPagina(), tarjetas.size());
        int fin = Math.min(inicio + consulta.tamanioPagina(), tarjetas.size());
        return new ResultadoPaginadoDTO<>(tarjetas.subList(inicio, fin), tarjetas.size(),
            consulta.pagina(), consulta.tamanioPagina());
    }

    private void validarConsulta(ConsultaRecomendacionesDTO consulta) throws ValidacionException {
        if (consulta == null)
            throw new ValidacionException("consulta", "Indica la página de recomendaciones.");
        if (consulta.pagina() < 1 || consulta.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La pagina y su tamaño deben ser positivos.");
    }

    private List<Integer> ordenarPorPuntuacion(List<Libro> libros, Map<Integer, Double> puntos, Set<Integer>
        excluir) {
        List<Integer> ids = new ArrayList<>();
        for (Libro libro : libros) {
            if (!excluir.contains(libro.getId()) && puntos.containsKey(libro.getId())) {
                ids.add(libro.getId());
            }
        }
        ids.sort((primero, segundo) -> {
            int comparacion = Double.compare(puntos.get(segundo), puntos.get(primero));
            if (comparacion == 0)
                return Integer.compare(primero, segundo);
            return comparacion;
        });
        return ids;
    }

    private record ConsultaGuardada(int propietario, List<Integer> ids) {
    }
}
