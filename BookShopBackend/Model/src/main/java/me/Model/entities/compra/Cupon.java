package me.Model.entities.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import me.Model.entities.usuario.Usuario;

public class Cupon {
    private int id;
    private Usuario usuario;
    private BigDecimal porcentaje;
    private BigDecimal montoMinimo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean activo;
    private boolean consumido;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public Cupon() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Cupon(int id, Usuario usuario, BigDecimal porcentaje, BigDecimal montoMinimo,
                 LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        this();
        this.id = id;
        this.usuario = usuario;
        this.porcentaje = porcentaje;
        this.montoMinimo = montoMinimo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    public BigDecimal getMontoMinimo() {
        return montoMinimo;
    }

    public void setMontoMinimo(BigDecimal montoMinimo) {
        this.montoMinimo = montoMinimo;
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

    public boolean isConsumido() {
        return consumido;
    }

    public void setConsumido(boolean consumido) {
        this.consumido = consumido;
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
        return fechaActual != null && fechaInicio != null && fechaFin != null
                && !fechaActual.isBefore(fechaInicio) && !fechaActual.isAfter(fechaFin);
    }

    public boolean esAplicable(BigDecimal subtotal, LocalDateTime fechaActual) {
        return activo && !eliminado && !consumido && estaVigente(fechaActual)
                && subtotal != null && subtotal.signum() >= 0 && montoMinimo != null
                && montoMinimo.signum() >= 0 && subtotal.compareTo(montoMinimo) >= 0
                && porcentaje != null && porcentaje.signum() > 0
                && porcentaje.compareTo(new BigDecimal("100")) <= 0;
    }

    public BigDecimal calcularDescuento(BigDecimal subtotal, LocalDateTime fechaActual) {
        if (!esAplicable(subtotal, fechaActual)) return BigDecimal.ZERO;
        return subtotal.multiply(porcentaje).divide(new BigDecimal("100"));
    }

    public void consumir(BigDecimal subtotal, LocalDateTime fechaActual) {
        if (!esAplicable(subtotal, fechaActual)) {
            throw new IllegalStateException("El cupon no es aplicable.");
        }
        consumido = true;
    }

    public void liberar(LocalDateTime fechaActual) {
        consumido = false;
        activo = !eliminado && estaVigente(fechaActual);
    }
}
