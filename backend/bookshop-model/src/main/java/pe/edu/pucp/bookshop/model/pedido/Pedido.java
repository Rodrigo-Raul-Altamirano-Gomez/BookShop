package pe.edu.pucp.bookshop.model.pedido;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.bookshop.model.usuario.Direccion;
import pe.edu.pucp.bookshop.model.usuario.MetodoPago;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

// El número del pedido (N.º 1045) es su id
public class Pedido {

    private Integer id;
    private Usuario usuario;
    private Direccion direccion;
    private MetodoPago metodoPago;
    private Cupon cupon;           // opcional
    private EstadoPedido estado;
    private LocalDateTime fechaCompra;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaCancelacion;
    private LocalDateTime fechaDevolucion;
    private String codigoTransaccion;
    private String codigoSeguimiento;
    private Double subtotal;
    private Double descuentoCupon;
    private Double costoEnvio;
    private Double total;
    private Boolean contadoParaCupon;
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {
    }

    // Cantidad de libros del pedido: la suma de las unidades de sus detalles
    public Integer getUnidades() {
        int unidades = 0;
        for (DetallePedido detalle : detalles) {
            unidades += detalle.getCantidad();
        }
        return unidades;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Direccion getDireccion() { return direccion; }
    public void setDireccion(Direccion direccion) { this.direccion = direccion; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public Cupon getCupon() { return cupon; }
    public void setCupon(Cupon cupon) { this.cupon = cupon; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public LocalDateTime getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(LocalDateTime fechaCompra) { this.fechaCompra = fechaCompra; }

    public LocalDateTime getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDateTime fechaSalida) { this.fechaSalida = fechaSalida; }

    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public LocalDateTime getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(LocalDateTime fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }

    public LocalDateTime getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDateTime fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }

    public String getCodigoTransaccion() { return codigoTransaccion; }
    public void setCodigoTransaccion(String codigoTransaccion) { this.codigoTransaccion = codigoTransaccion; }

    public String getCodigoSeguimiento() { return codigoSeguimiento; }
    public void setCodigoSeguimiento(String codigoSeguimiento) { this.codigoSeguimiento = codigoSeguimiento; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getDescuentoCupon() { return descuentoCupon; }
    public void setDescuentoCupon(Double descuentoCupon) { this.descuentoCupon = descuentoCupon; }

    public Double getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(Double costoEnvio) { this.costoEnvio = costoEnvio; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Boolean getContadoParaCupon() { return contadoParaCupon; }
    public void setContadoParaCupon(Boolean contadoParaCupon) { this.contadoParaCupon = contadoParaCupon; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }
}
