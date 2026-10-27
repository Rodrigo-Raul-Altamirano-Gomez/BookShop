package me.Model.entities.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.Model.entities.compra.DireccionEnvio;
import me.Model.entities.compra.MetodoPago;
import me.Model.entities.usuario.Usuario;

public class Pedido {
    private int id;
    private Usuario usuario;
    private List<Item> items;
    private Cupon cupon;
    private BigDecimal subtotalProductos;
    private BigDecimal descuentoLibros;
    private BigDecimal porcentajeCupon;
    private BigDecimal descuentoCupon;
    private BigDecimal porcentajeEnvio;
    private BigDecimal costoEnvio;
    private BigDecimal total;
    private int cantidadTotalLibros;
    private DireccionEnvio direccionEnvio;
    private MetodoPago metodoPago;
    private String identificadorTransaccion;
    private LocalDateTime fechaPago;
    private EstadoPedido estado;
    private String codigoSeguimiento;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEntrega;
    private int numeroIntentos;
    private boolean activo;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public Pedido() {
        this.items = new ArrayList<>();
        this.estado = EstadoPedido.EN_ESPERA;
        this.activo = true;
        this.subtotalProductos = BigDecimal.ZERO;
        this.descuentoLibros = BigDecimal.ZERO;
        this.porcentajeCupon = BigDecimal.ZERO;
        this.descuentoCupon = BigDecimal.ZERO;
        this.porcentajeEnvio = BigDecimal.ZERO;
        this.costoEnvio = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Pedido(int id, Usuario usuario, Carrito carrito) {
        this();
        this.id = id;
        this.usuario = usuario;
        for (Item item : carrito.getItems()) {
            if (item.isEliminado()) continue;
            this.items.add(new Item(item));
            subtotalProductos = subtotalProductos.add(item.getPrecioBase().multiply(
                    BigDecimal.valueOf(item.getCantidad())));
            descuentoLibros = descuentoLibros.add(item.getDescuentoTotal());
        }
        total = carrito.getTotal();
        cantidadTotalLibros = carrito.getCantidad();
    }

    public Pedido(int id, Usuario usuario, Carrito carrito, DireccionEnvio direccionEnvio) {
        this(id, usuario, carrito);
        this.direccionEnvio = direccionEnvio;
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

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public void setItems(List<Item> items) {
        this.items = new ArrayList<>(items);
    }

    public Cupon getCupon() {
        return cupon;
    }

    public void setCupon(Cupon cupon) {
        this.cupon = cupon;
    }

    public BigDecimal getSubtotalProductos() {
        return subtotalProductos;
    }

    public void setSubtotalProductos(BigDecimal subtotalProductos) {
        this.subtotalProductos = subtotalProductos;
    }

    public BigDecimal getDescuentoLibros() {
        return descuentoLibros;
    }

    public void setDescuentoLibros(BigDecimal descuentoLibros) {
        this.descuentoLibros = descuentoLibros;
    }

    public BigDecimal getPorcentajeCupon() {
        return porcentajeCupon;
    }

    public void setPorcentajeCupon(BigDecimal porcentajeCupon) {
        this.porcentajeCupon = porcentajeCupon;
    }

    public BigDecimal getDescuentoCupon() {
        return descuentoCupon;
    }

    public void setDescuentoCupon(BigDecimal descuentoCupon) {
        this.descuentoCupon = descuentoCupon;
    }

    public BigDecimal getPorcentajeEnvio() {
        return porcentajeEnvio;
    }

    public void setPorcentajeEnvio(BigDecimal porcentajeEnvio) {
        this.porcentajeEnvio = porcentajeEnvio;
    }

    public BigDecimal getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(BigDecimal costoEnvio) {
        this.costoEnvio = costoEnvio;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public int getCantidadTotalLibros() {
        return cantidadTotalLibros;
    }

    public void setCantidadTotalLibros(int cantidadTotalLibros) {
        this.cantidadTotalLibros = cantidadTotalLibros;
    }

    public DireccionEnvio getDireccionEnvio() {
        return direccionEnvio;
    }

    public void setDireccionEnvio(DireccionEnvio direccionEnvio) {
        this.direccionEnvio = direccionEnvio;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getIdentificadorTransaccion() {
        return identificadorTransaccion;
    }

    public void setIdentificadorTransaccion(String identificadorTransaccion) {
        this.identificadorTransaccion = identificadorTransaccion;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public String getCodigoSeguimiento() {
        return codigoSeguimiento;
    }

    public void setCodigoSeguimiento(String codigoSeguimiento) {
        this.codigoSeguimiento = codigoSeguimiento;
    }

    public LocalDateTime getFechaEntregaEstimada() {
        return fechaEntregaEstimada;
    }

    public void setFechaEntregaEstimada(LocalDateTime fechaEntregaEstimada) {
        this.fechaEntregaEstimada = fechaEntregaEstimada;
    }

    public LocalDateTime getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public int getNumeroIntentos() {
        return numeroIntentos;
    }

    public void setNumeroIntentos(int numeroIntentos) {
        if (numeroIntentos < 0) {
            throw new IllegalArgumentException("El numero de intentos no puede ser negativo.");
        }
        this.numeroIntentos = numeroIntentos;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
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

    public BigDecimal getSubtotal() {
        return subtotalProductos.subtract(descuentoLibros);
    }

    public boolean puedeCancelar() {
        return !eliminado && estado == EstadoPedido.EN_ALMACEN;
    }

    public boolean puedeSolicitarDevolucion(LocalDateTime fechaActual, int plazoDias, int maximoIntentos) {
        return !eliminado && estado == EstadoPedido.ENTREGADO && fechaEntrega != null
                && fechaActual != null && plazoDias >= 0 && maximoIntentos > 0
                && numeroIntentos < maximoIntentos && !fechaActual.isBefore(fechaEntrega)
                && !fechaActual.isAfter(fechaEntrega.plusDays(plazoDias));
    }

    public void registrarIntento() {
        numeroIntentos = Math.incrementExact(numeroIntentos);
    }
}
