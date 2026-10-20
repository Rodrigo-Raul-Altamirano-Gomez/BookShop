package me.Model.entities.compra;

import java.time.LocalDateTime;
import me.Model.entities.usuario.Usuario;

public class DireccionEnvio {
    private int id;
    private Usuario usuario;
    private String alias;
    private String calle;
    private String distrito;
    private String ciudad;
    private String referencia;
    private boolean activo;
    private boolean predeterminada;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public DireccionEnvio() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public DireccionEnvio(int id, Usuario usuario, String calle, String distrito, String ciudad,
                          String referencia) {
        this(id, usuario, null, calle, distrito, ciudad, referencia);
    }

    public DireccionEnvio(int id, Usuario usuario, String alias, String calle, String distrito,
                          String ciudad, String referencia) {
        this();
        this.id = id;
        this.usuario = usuario;
        this.alias = alias;
        this.calle = calle;
        this.distrito = distrito;
        this.ciudad = ciudad;
        this.referencia = referencia;
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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
        if (!activo) predeterminada = false;
    }

    public boolean isPredeterminada() {
        return predeterminada;
    }

    public void setPredeterminada(boolean predeterminada) {
        if (predeterminada && !estaDisponible()) {
            throw new IllegalStateException("Una direccion no disponible no puede ser predeterminada.");
        }
        this.predeterminada = predeterminada;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
        if (eliminado) predeterminada = false;
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

    public boolean estaDisponible() {
        return activo && !eliminado;
    }
}
