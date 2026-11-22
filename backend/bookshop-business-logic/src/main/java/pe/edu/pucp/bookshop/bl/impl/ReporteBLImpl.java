package pe.edu.pucp.bookshop.bl.impl;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import pe.edu.pucp.bookshop.bl.ReporteBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.exception.PersistenciaException;
import pe.edu.pucp.bookshop.dao.PedidoDAO;
import pe.edu.pucp.bookshop.dao.UsuarioDAO;
import pe.edu.pucp.bookshop.dao.impl.PedidoDAOImpl;
import pe.edu.pucp.bookshop.dao.impl.UsuarioDAOImpl;
import pe.edu.pucp.bookshop.model.EstadoPedido;
import pe.edu.pucp.bookshop.model.Rol;
import pe.edu.pucp.bookshop.model.Usuario;
import pe.edu.pucp.bookshop.model.dto.Venta;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ReporteBLImpl implements ReporteBL {

    private PedidoDAO pedidoDAO = new PedidoDAOImpl();
    private UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    // RF18: libros con ventas válidas en el período, por monto vendido (suma de los subtotales de sus detalles)
    @Override
    public byte[] generarReporteLibros(LocalDate desde, LocalDate hasta, Integer limite) throws BusinessLogicException {
        try {
            Map<Integer, Map<String, Object>> porLibro = new HashMap<>();
            for (Venta venta : pedidoDAO.listarVentas(EstadoPedido.VENTA_VALIDA, desde, hasta)) {
                Map<String, Object> fila = porLibro.get(venta.libro().getId());
                if (fila == null) {
                    fila = new HashMap<>();
                    fila.put("titulo", venta.libro().getTitulo());
                    fila.put("categoria", venta.libro().getCategoria().getNombre());
                    fila.put("autor", venta.libro().getAutor().getNombre());
                    fila.put("editorial", venta.libro().getEditorial().getNombre());
                    fila.put("cantidad", 0);
                    fila.put("monto", 0.0);
                    porLibro.put(venta.libro().getId(), fila);
                }
                fila.put("cantidad", (Integer) fila.get("cantidad") + venta.cantidad());
                fila.put("monto", Math.round(((Double) fila.get("monto") + venta.subtotal()) * 100) / 100.0);
            }
            List<Map<String, Object>> filas = new ArrayList<>(porLibro.values());
            filas.sort((a, b) -> Double.compare((Double) b.get("monto"), (Double) a.get("monto")));
            if (limite != null && filas.size() > limite) {
                filas = filas.subList(0, limite);
            }
            return generarPdf("/reportes/reporte-libros.jasper", desde, hasta, filas);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // RF19: todos los clientes, también los que no compraron en el período, por gasto (suma de los totales de sus
    // pedidos válidos del período). Categoría, autor y editorial: los más comprados en el período, por unidades.
    @Override
    public byte[] generarReporteClientes(LocalDate desde, LocalDate hasta, Integer limite) throws BusinessLogicException {
        try {
            Map<Integer, Set<Integer>> pedidosContados = new HashMap<>();
            Map<Integer, Double> gasto = new HashMap<>();
            Map<Integer, Map<String, Integer>> categorias = new HashMap<>();
            Map<Integer, Map<String, Integer>> autores = new HashMap<>();
            Map<Integer, Map<String, Integer>> editoriales = new HashMap<>();
            for (Venta venta : pedidoDAO.listarVentas(EstadoPedido.VENTA_VALIDA, desde, hasta)) {
                int cliente = venta.idUsuario();
                if (!pedidosContados.containsKey(cliente)) {
                    pedidosContados.put(cliente, new HashSet<>());
                    categorias.put(cliente, new HashMap<>());
                    autores.put(cliente, new HashMap<>());
                    editoriales.put(cliente, new HashMap<>());
                }
                // El total de cada pedido se suma una sola vez, aunque tenga varios libros
                if (pedidosContados.get(cliente).add(venta.idPedido())) {
                    gasto.put(cliente, gasto.getOrDefault(cliente, 0.0) + venta.totalPedido());
                }
                String categoria = venta.libro().getCategoria().getNombre();
                String autor = venta.libro().getAutor().getNombre();
                String editorial = venta.libro().getEditorial().getNombre();
                categorias.get(cliente).put(categoria, categorias.get(cliente).getOrDefault(categoria, 0) + venta.cantidad());
                autores.get(cliente).put(autor, autores.get(cliente).getOrDefault(autor, 0) + venta.cantidad());
                editoriales.get(cliente).put(editorial, editoriales.get(cliente).getOrDefault(editorial, 0) + venta.cantidad());
            }

            LocalDate hoy = LocalDate.now();
            List<Map<String, Object>> filas = new ArrayList<>();
            for (Usuario cliente : usuarioDAO.listarPorRol(Rol.CLIENTE)) {
                int id = cliente.getId();
                Map<String, Object> fila = new HashMap<>();
                fila.put("nombre", cliente.getNombreCompleto());
                fila.put("correo", cliente.getCorreo());
                fila.put("estado", cliente.getActivo() ? "Activa" : "Desactivada");
                fila.put("diasUso", (int) ChronoUnit.DAYS.between(cliente.getFechaRegistro().toLocalDate(), hoy));
                fila.put("gasto", Math.round(gasto.getOrDefault(id, 0.0) * 100) / 100.0);
                fila.put("categoria", masComprado(categorias.get(id)));
                fila.put("autor", masComprado(autores.get(id)));
                fila.put("editorial", masComprado(editoriales.get(id)));
                filas.add(fila);
            }
            filas.sort((a, b) -> Double.compare((Double) b.get("gasto"), (Double) a.get("gasto")));
            if (limite != null && filas.size() > limite) {
                filas = filas.subList(0, limite);
            }
            return generarPdf("/reportes/reporte-clientes.jasper", desde, hasta, filas);
        } catch (SQLException ex) {
            throw new PersistenciaException(ex);
        }
    }

    // El nombre con más unidades; "—" si el cliente no compró en el período (RF19.3)
    private String masComprado(Map<String, Integer> unidades) {
        String nombre = "—";
        int maximo = 0;
        if (unidades != null) {
            for (String clave : unidades.keySet()) {
                if (unidades.get(clave) > maximo) {
                    maximo = unidades.get(clave);
                    nombre = clave;
                }
            }
        }
        return nombre;
    }

    // Llena la plantilla con las filas y el encabezado común (período, fecha de generación y cantidad de resultados)
    private byte[] generarPdf(String plantilla, LocalDate desde, LocalDate hasta, List<Map<String, Object>> filas) {
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodo = (desde != null ? desde.format(fecha) : "el inicio") + " – " + (hasta != null ? hasta.format(fecha) : "hoy");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.REPORT_LOCALE, Locale.US);   // S/ 1,234.50 (UX-UI §12.1)
        parametros.put("periodo", periodo);
        parametros.put("generado", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        parametros.put("resultados", filas.size());
        try {
            JasperReport reporte = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream(plantilla));
            JasperPrint documento = JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(new ArrayList<>(filas)));
            return JasperExportManager.exportReportToPdf(documento);
        } catch (JRException ex) {
            // Un error de la plantilla no es del uso normal: se informa como fallo del servidor
            throw new RuntimeException(ex);
        }
    }
}
