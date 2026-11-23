package pe.edu.pucp.bookshop.model.reporte;

// Una fila del reporte de libros por monto vendido (RF18). Es una clase con getters y no un record porque
// JasperReports lee los campos de la plantilla con getX()
public class FilaReporteLibro {

    private String titulo;
    private String categoria;
    private String autor;
    private String editorial;
    private Integer cantidad;
    private Double monto;

    public FilaReporteLibro() {
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public String getEditorial() { return editorial; }
    public void setEditorial(String editorial) { this.editorial = editorial; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
}
