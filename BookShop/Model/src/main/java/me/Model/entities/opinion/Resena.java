package me.Model.entities.opinion;

import java.time.LocalDateTime;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.Usuario;

public class Resena {
    private int id;
    private Libro libro;
    private Usuario usuario;
    private int puntuacion;
    private String comentario;
    private LocalDateTime fecha;
    private boolean activo;
    private int cantidadVotosUtiles;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public Resena() {
        this.fecha = LocalDateTime.now();
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Resena(int id, int puntuacion, String comentario, Libro libro, Usuario usuario) {
        this();
        this.id = id;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.libro = libro;
        this.usuario = usuario;
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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getCantidadVotosUtiles() {
        return cantidadVotosUtiles;
    }

    public void setCantidadVotosUtiles(int cantidadVotosUtiles) {
        if (cantidadVotosUtiles < 0) {
            throw new IllegalArgumentException("La cantidad de votos no puede ser negativa.");
        }
        this.cantidadVotosUtiles = cantidadVotosUtiles;
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

    public boolean estaDisponible() {
        return activo && !eliminado;
    }
}
