package me.Model.entities.catalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Descuento {
    private int id;
    private Libro libro;
    private BigDecimal porcentaje;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean activo;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public Descuento() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Descuento(int id, Libro libro, BigDecimal porcentaje,
                     LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        this();
        this.id = id;
        this.libro = libro;
        this.porcentaje = porcentaje;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    public void setUsuarioCreacion(String usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getUsuarioActualizacion() {
        return usuarioActualizacion;
    }

    public void setUsuarioActualizacion(String usuarioActualizacion) {
        this.usuarioActualizacion = usuarioActualizacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public boolean estaVigente(LocalDateTime fechaActual) {
        return activo && !eliminado && fechaActual != null && fechaInicio != null && fechaFin != null
                && !fechaActual.isBefore(fechaInicio) && !fechaActual.isAfter(fechaFin);
    }

    public BigDecimal calcularPrecioFinal(BigDecimal precioOriginal, LocalDateTime fechaActual) {
        if (precioOriginal == null || precioOriginal.signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser no negativo.");
        }
        if (!estaVigente(fechaActual)) return precioOriginal;
        if (porcentaje == null || porcentaje.signum() < 0 || porcentaje.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalStateException("El porcentaje de descuento no es valido.");
        }
        return precioOriginal.subtract(precioOriginal.multiply(porcentaje).divide(new BigDecimal("100")));
    }
}
