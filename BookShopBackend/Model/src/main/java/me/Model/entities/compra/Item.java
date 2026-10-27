package me.Model.entities.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import me.Model.entities.catalogo.Libro;

public class Item {
    private int id;
    private Libro libro;
    private int cantidad;
    private BigDecimal precioBase;
    private BigDecimal precioUnitario;
    private BigDecimal porcentajeDescuento;
    private BigDecimal subTotal;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public Item() {
        this.precioBase = BigDecimal.ZERO;
        this.precioUnitario = BigDecimal.ZERO;
        this.porcentajeDescuento = BigDecimal.ZERO;
        this.subTotal = BigDecimal.ZERO;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Item(int id, Libro libro, int cantidad) {
        this();
        this.id = id;
        this.libro = libro;
        this.cantidad = cantidad;
        this.precioBase = libro.getPrecioBase();
        this.precioUnitario = precioBase;
        recalcularSubTotal();
    }

    public Item(Item original) {
        this.id = original.id;
        this.libro = original.libro;
        this.cantidad = original.cantidad;
        this.precioBase = original.precioBase;
        this.precioUnitario = original.precioUnitario;
        this.porcentajeDescuento = original.porcentajeDescuento;
        this.subTotal = original.subTotal;
        this.eliminado = original.eliminado;
        this.usuarioCreacion = original.usuarioCreacion;
        this.fechaCreacion = original.fechaCreacion;
        this.usuarioActualizacion = original.usuarioActualizacion;
        this.fechaActualizacion = original.fechaActualizacion;
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

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
        this.cantidad = cantidad;
        recalcularSubTotal();
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
        recalcularSubTotal();
    }

    public BigDecimal getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public void setPorcentajeDescuento(BigDecimal porcentajeDescuento) {
        this.porcentajeDescuento = porcentajeDescuento;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
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

    public BigDecimal getDescuentoTotal() {
        if (precioBase == null || precioUnitario == null) return BigDecimal.ZERO;
        return precioBase.subtract(precioUnitario).multiply(BigDecimal.valueOf(cantidad));
    }

    private void recalcularSubTotal() {
        subTotal = precioUnitario == null ? BigDecimal.ZERO
                : precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
