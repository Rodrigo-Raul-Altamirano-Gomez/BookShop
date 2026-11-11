package me.Model.entities.solicitud;

import java.time.LocalDateTime;
import me.Model.entities.usuario.Usuario;

public abstract class Solicitud {

    private int id;
    private Usuario usuario;
    private String comentario;
    private EstadoSolicitud estado;
    private LocalDateTime fechaResolucion;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public Solicitud() {
        this.estado = EstadoSolicitud.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Solicitud(int id, Usuario usuario, String comentario) {
        this();
        this.id = id;
        this.usuario = usuario;
        this.comentario = comentario;
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

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
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

    public boolean estaPendiente() {
        return !eliminado && estado == EstadoSolicitud.PENDIENTE;
    }

    public boolean estaResuelta() {
        return estado != null && estado != EstadoSolicitud.PENDIENTE;
    }

    public void editar(String comentario, String correo, LocalDateTime fecha) {
        this.comentario = comentario;
        this.usuarioActualizacion = correo;
        this.fechaActualizacion = fecha;
    }

    public void retirar(String correo, LocalDateTime fecha) {
        eliminado = true;
        usuarioActualizacion = correo;
        fechaActualizacion = fecha;
    }

    public void resolver(EstadoSolicitud resultado, String correo, LocalDateTime fecha) {
        estado = resultado;
        fechaResolucion = fecha;
        usuarioActualizacion = correo;
        fechaActualizacion = fecha;
    }
}
