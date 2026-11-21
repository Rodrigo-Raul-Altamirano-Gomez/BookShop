package pe.edu.pucp.bookshop.model;

import java.time.LocalDate;

public class Descuento {

    private Integer id;
    private Libro libro;
    private Integer porcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoDescuento estado;   // lo calcula la lógica de negocio con las fechas (ERS §2.3)

    public Descuento() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Libro getLibro() { return libro; }
    public void setLibro(Libro libro) { this.libro = libro; }

    public Integer getPorcentaje() { return porcentaje; }
    public void setPorcentaje(Integer porcentaje) { this.porcentaje = porcentaje; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public EstadoDescuento getEstado() { return estado; }
    public void setEstado(EstadoDescuento estado) { this.estado = estado; }
}
