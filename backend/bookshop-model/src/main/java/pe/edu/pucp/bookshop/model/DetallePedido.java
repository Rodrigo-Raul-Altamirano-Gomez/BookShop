package pe.edu.pucp.bookshop.model;

// Un libro del pedido, con el precio y el descuento del momento de la compra
public class DetallePedido {

    private Integer id;
    private Libro libro;
    private Integer cantidad;
    private Double precioUnitario;       // precio sin descuento
    private Double descuentoUnitario;    // rebaja por unidad del descuento vigente
    private Double subtotal;

    public DetallePedido() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Libro getLibro() { return libro; }
    public void setLibro(Libro libro) { this.libro = libro; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }

    public Double getDescuentoUnitario() { return descuentoUnitario; }
    public void setDescuentoUnitario(Double descuentoUnitario) { this.descuentoUnitario = descuentoUnitario; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
}
