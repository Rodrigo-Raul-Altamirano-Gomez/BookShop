package pe.edu.pucp.bookshop.model.resena;

import java.time.LocalDateTime;

import pe.edu.pucp.bookshop.model.comun.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

// Reporte de una reseña ajena (ERS RF10.6–8). Guarda una copia del texto y la puntuación del momento.
public class ReporteResena {

    private Integer id;
    private Usuario usuario;              // quien reporta
    private Resena resena;
    private MotivoReporte motivo;
    private String comentario;
    private String contenidoReportado;
    private Integer puntuacionReportada;
    private EstadoSolicitud estado;
    private LocalDateTime fecha;

    public ReporteResena() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Resena getResena() { return resena; }
    public void setResena(Resena resena) { this.resena = resena; }

    public MotivoReporte getMotivo() { return motivo; }
    public void setMotivo(MotivoReporte motivo) { this.motivo = motivo; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public String getContenidoReportado() { return contenidoReportado; }
    public void setContenidoReportado(String contenidoReportado) { this.contenidoReportado = contenidoReportado; }

    public Integer getPuntuacionReportada() { return puntuacionReportada; }
    public void setPuntuacionReportada(Integer puntuacionReportada) { this.puntuacionReportada = puntuacionReportada; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
