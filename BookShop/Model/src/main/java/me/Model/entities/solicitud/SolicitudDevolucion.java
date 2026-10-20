package me.Model.entities.solicitud;

import java.time.LocalDateTime;
import me.Model.entities.usuario.Usuario;
import me.Model.entities.compra.Pedido;

public class SolicitudDevolucion extends Solicitud {
    private Pedido pedido;
    private MotivoDevolucion motivo;

    public SolicitudDevolucion() {
    }

    public SolicitudDevolucion(int id, Usuario usuario, Pedido pedido, MotivoDevolucion motivo, String comentario) {
        super(id, usuario, comentario);
        this.pedido = pedido;
        this.motivo = motivo;
    }

    public MotivoDevolucion getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoDevolucion motivo) {
        this.motivo = motivo;
    }

    public void editar(MotivoDevolucion motivo, String comentario, String correo, LocalDateTime fecha) {
        super.editar(comentario, correo, fecha);
        this.motivo = motivo;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        if (this.pedido != null && (pedido == null || this.pedido.getId() != pedido.getId())) {
            throw new IllegalStateException("No se puede cambiar el pedido de la solicitud.");
        }
        this.pedido = pedido;
    }
}
