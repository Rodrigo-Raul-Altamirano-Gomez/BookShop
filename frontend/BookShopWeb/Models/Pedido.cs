namespace BookShopWeb.Models;

// El número del pedido (N.º 1045) es su Id
public class Pedido
{
    public int Id { get; set; }
    public Usuario Usuario { get; set; } = new();
    public Direccion Direccion { get; set; } = new();
    public MetodoPago MetodoPago { get; set; } = new();
    public Cupon? Cupon { get; set; }
    public EstadoPedido Estado { get; set; }
    public DateTime FechaCompra { get; set; }
    public DateTime? FechaSalida { get; set; }
    public DateTime? FechaEntrega { get; set; }
    public DateTime? FechaCancelacion { get; set; }
    public DateTime? FechaDevolucion { get; set; }
    public string CodigoTransaccion { get; set; } = "";
    public string CodigoSeguimiento { get; set; } = "";
    public decimal Subtotal { get; set; }
    public decimal DescuentoCupon { get; set; }
    public decimal CostoEnvio { get; set; }
    public decimal Total { get; set; }
    public List<DetallePedido> Detalles { get; set; } = new();
    public int Unidades { get; set; }   // la calcula el backend
}
