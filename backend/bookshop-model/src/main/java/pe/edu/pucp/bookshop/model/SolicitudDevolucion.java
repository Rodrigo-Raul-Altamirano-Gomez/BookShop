package pe.edu.pucp.bookshop.model;

import java.time.LocalDateTime;

// Devolución completa de un pedido entregado (ERS RF03.6–10)
public class SolicitudDevolucion {

    private Integer id;
    private Pedido pedido;
    private MotivoDevolucion motivo;
    private String comentario;
    private EstadoSolicitud estado;
    private LocalDateTime fecha;

    public SolicitudDevolucion() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public MotivoDevolucion getMotivo() { return motivo; }
    public void setMotivo(MotivoDevolucion motivo) { this.motivo = motivo; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
