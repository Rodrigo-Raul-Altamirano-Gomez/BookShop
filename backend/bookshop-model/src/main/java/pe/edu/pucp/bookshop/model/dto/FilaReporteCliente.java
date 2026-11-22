package pe.edu.pucp.bookshop.model.dto;

// Una fila del reporte de clientes por monto gastado (RF19). Es una clase con getters y no un record porque
// JasperReports lee los campos de la plantilla con getX()
public class FilaReporteCliente {

    private String nombre;
    private String correo;
    private String estado;
    private Integer diasUso;
    private Double gasto;
    private String categoria;
    private String autor;
    private String editorial;

    public FilaReporteCliente() {
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getDiasUso() { return diasUso; }
    public void setDiasUso(Integer diasUso) { this.diasUso = diasUso; }
    public Double getGasto() { return gasto; }
    public void setGasto(Double gasto) { this.gasto = gasto; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public String getEditorial() { return editorial; }
    public void setEditorial(String editorial) { this.editorial = editorial; }
}
