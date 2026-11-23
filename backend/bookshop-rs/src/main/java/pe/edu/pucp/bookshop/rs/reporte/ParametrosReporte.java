package pe.edu.pucp.bookshop.rs.reporte;

import jakarta.ws.rs.QueryParam;

import java.time.LocalDate;

// Parámetros de los dos reportes PDF (RF18 y RF19): período y límite de resultados opcional
public class ParametrosReporte {

    @QueryParam("desde") private String desde;
    @QueryParam("hasta") private String hasta;
    @QueryParam("limite") private Integer limite;

    public LocalDate getDesde() {
        return desde != null ? LocalDate.parse(desde) : null;
    }

    public LocalDate getHasta() {
        return hasta != null ? LocalDate.parse(hasta) : null;
    }

    public Integer getLimite() {
        return limite;
    }
}
