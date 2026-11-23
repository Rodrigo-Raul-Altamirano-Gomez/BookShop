package pe.edu.pucp.bookshop.model.resena;

import java.time.LocalDateTime;

import pe.edu.pucp.bookshop.model.catalogo.Libro;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

public class Resena {

    private Integer id;
    private Usuario usuario;
    private Libro libro;
    private Integer puntuacion;       // 1 a 5
    private String comentario;
    private LocalDateTime fecha;
    private Boolean activa;           // false: oculta por moderación
    private Integer votosUtiles;      // cantidad de votos útiles
    private Boolean votadaPorMi;      // si quien consulta ya la marcó como útil

    public Resena() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Libro getLibro() { return libro; }
    public void setLibro(Libro libro) { this.libro = libro; }

    public Integer getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Integer puntuacion) { this.puntuacion = puntuacion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }

    public Integer getVotosUtiles() { return votosUtiles; }
    public void setVotosUtiles(Integer votosUtiles) { this.votosUtiles = votosUtiles; }

    public Boolean getVotadaPorMi() { return votadaPorMi; }
    public void setVotadaPorMi(Boolean votadaPorMi) { this.votadaPorMi = votadaPorMi; }
}
