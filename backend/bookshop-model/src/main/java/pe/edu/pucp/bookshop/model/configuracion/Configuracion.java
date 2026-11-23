package pe.edu.pucp.bookshop.model.configuracion;

// Parámetros que edita el administrador (ERS §3.1)
public class Configuracion {

    private Double costoEnvio;
    private Integer diasDevolucion;
    private Integer maxSolicitudesDevolucion;
    private Integer maxCancelacionesMes;
    private Double cuponMontoMinimo;
    private Integer cuponCadaPedidos;
    private Integer cuponPorcentaje;
    private Integer cuponDiasVigencia;

    public Configuracion() {
    }

    public Double getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(Double costoEnvio) { this.costoEnvio = costoEnvio; }

    public Integer getDiasDevolucion() { return diasDevolucion; }
    public void setDiasDevolucion(Integer diasDevolucion) { this.diasDevolucion = diasDevolucion; }

    public Integer getMaxSolicitudesDevolucion() { return maxSolicitudesDevolucion; }
    public void setMaxSolicitudesDevolucion(Integer maxSolicitudesDevolucion) { this.maxSolicitudesDevolucion = maxSolicitudesDevolucion; }

    public Integer getMaxCancelacionesMes() { return maxCancelacionesMes; }
    public void setMaxCancelacionesMes(Integer maxCancelacionesMes) { this.maxCancelacionesMes = maxCancelacionesMes; }

    public Double getCuponMontoMinimo() { return cuponMontoMinimo; }
    public void setCuponMontoMinimo(Double cuponMontoMinimo) { this.cuponMontoMinimo = cuponMontoMinimo; }

    public Integer getCuponCadaPedidos() { return cuponCadaPedidos; }
    public void setCuponCadaPedidos(Integer cuponCadaPedidos) { this.cuponCadaPedidos = cuponCadaPedidos; }

    public Integer getCuponPorcentaje() { return cuponPorcentaje; }
    public void setCuponPorcentaje(Integer cuponPorcentaje) { this.cuponPorcentaje = cuponPorcentaje; }

    public Integer getCuponDiasVigencia() { return cuponDiasVigencia; }
    public void setCuponDiasVigencia(Integer cuponDiasVigencia) { this.cuponDiasVigencia = cuponDiasVigencia; }
}
