package me.Model.entities.compra;

import java.time.LocalDate;
import java.time.LocalDateTime;
import me.Model.entities.usuario.Usuario;

public abstract class MetodoPago {
    private int id;
    private Usuario usuario;
    private String titular;
    private String proveedor;
    private String alias;
    private boolean activo;
    private boolean predeterminado;
    private LocalDate fechaVencimiento;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public MetodoPago() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public MetodoPago(int id, String titular, String proveedor, Usuario usuario, String alias,
                      LocalDate fechaVencimiento) {
        this();
        this.id = id;
        this.usuario = usuario;
        this.titular = titular;
        this.proveedor = proveedor;
        this.alias = alias;
        this.fechaVencimiento = fechaVencimiento;
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

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
        if (!activo) predeterminado = false;
    }

    public boolean isPredeterminado() {
        return predeterminado;
    }

    public void setPredeterminado(boolean predeterminado) {
        this.predeterminado = predeterminado;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
        if (eliminado) predeterminado = false;
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

    public boolean validarEstado() {
        return validarEstado(LocalDate.now());
    }

    public boolean validarEstado(LocalDate fechaActual) {
        return activo && !eliminado && fechaActual != null && fechaVencimiento != null
                && !fechaVencimiento.isBefore(fechaActual);
    }

    public boolean estaVencido(LocalDate fechaActual) {
        return fechaActual != null && fechaVencimiento != null && fechaVencimiento.isBefore(fechaActual);
    }
}
