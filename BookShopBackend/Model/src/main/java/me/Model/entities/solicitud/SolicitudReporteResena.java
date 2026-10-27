package me.Model.entities.solicitud;

import java.time.LocalDateTime;
import me.Model.entities.opinion.Resena;
import me.Model.entities.usuario.Usuario;

public class SolicitudReporteResena extends Solicitud {
    private Resena resena;
    private MotivoReporteResena motivo;
    private String comentarioOriginal;
    private int puntuacionOriginal;

    public SolicitudReporteResena() {
    }

    public SolicitudReporteResena(int id, Usuario usuario, Resena resena,
                                 MotivoReporteResena motivo, String comentario) {
        super(id, usuario, comentario);
        this.resena = resena;
        this.motivo = motivo;
        this.comentarioOriginal = resena.getComentario();
        this.puntuacionOriginal = resena.getPuntuacion();
    }

    public MotivoReporteResena getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoReporteResena motivo) {
        this.motivo = motivo;
    }

    public void editar(MotivoReporteResena motivo, String comentario, String correo, LocalDateTime fecha) {
        super.editar(comentario, correo, fecha);
        this.motivo = motivo;
    }

    public Resena getResena() {
        return resena;
    }

    public void setResena(Resena resena) {
        if (this.resena != null && (resena == null || this.resena.getId() != resena.getId())) {
            throw new IllegalStateException("No se puede cambiar la resena de la solicitud.");
        }
        this.resena = resena;
    }

    public String getComentarioOriginal() {
        return comentarioOriginal;
    }

    public void setComentarioOriginal(String comentarioOriginal) {
        this.comentarioOriginal = comentarioOriginal;
    }

    public int getPuntuacionOriginal() {
        return puntuacionOriginal;
    }

    public void setPuntuacionOriginal(int puntuacionOriginal) {
        this.puntuacionOriginal = puntuacionOriginal;
    }

    public String getComentarioActual() {
        return resena == null ? null : resena.getComentario();
    }

    public boolean isResenaActiva() {
        return resena != null && resena.estaDisponible();
    }
}
