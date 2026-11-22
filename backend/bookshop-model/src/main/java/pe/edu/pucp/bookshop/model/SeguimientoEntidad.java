package pe.edu.pucp.bookshop.model;

import java.time.LocalDateTime;

// Un autor, una categoría o una editorial que sigue el cliente (ERS RF05.3)
public class SeguimientoEntidad {

    private Usuario usuario;
    private Entidad entidad;
    private LocalDateTime fecha;

    public SeguimientoEntidad() {
    }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
