package pe.edu.pucp.bookshop.rs.catalogo;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import pe.edu.pucp.bookshop.model.catalogo.FiltroLibros;

import java.time.LocalDate;
import java.util.List;

// Parámetros de la URL de toda vista de resultados de libros (los mismos nombres que usa el front)
public class ParametrosLibros {

    @QueryParam("q") private String q;
    @QueryParam("categoria") private List<Integer> categorias;
    @QueryParam("autor") private List<Integer> autores;
    @QueryParam("editorial") private List<Integer> editoriales;
    @QueryParam("idioma") private String idioma;
    @QueryParam("min") private Double min;
    @QueryParam("max") private Double max;
    @QueryParam("desde") private String desde;
    @QueryParam("hasta") private String hasta;
    @QueryParam("orden") private String orden;
    @QueryParam("pagina") @DefaultValue("1") private int pagina;
    @QueryParam("tamano") @DefaultValue("12") private int tamano;

    public FiltroLibros aFiltro() {
        LocalDate fechaDesde = null;
        if (desde != null) {
            fechaDesde = LocalDate.parse(desde);
        }
        LocalDate fechaHasta = null;
        if (hasta != null) {
            fechaHasta = LocalDate.parse(hasta);
        }
        return new FiltroLibros(q, categorias, autores, editoriales, idioma, min, max, fechaDesde, fechaHasta, orden, pagina, tamano);
    }
}
