namespace Web.Modelos;

public class Pedido
{
    public int Id { get; set; }
    public DateTime FechaPago { get; set; }
    public string Estado { get; set; } = "En almacén";
    public List<LineaPedido> Lineas { get; set; } = [];
    public Direccion Direccion { get; set; } = new();
    public MetodoPago Metodo { get; set; } = new();
    public decimal Productos { get; set; }
    public decimal DescuentoLibro { get; set; }
    public decimal Subtotal { get; set; }
    public decimal DescuentoCupon { get; set; }
    public decimal Envio { get; set; }
    public decimal Total { get; set; }
    public string Transaccion { get; set; } = "";
    public string Seguimiento { get; set; } = "";
    public DateTime EntregaEstimada { get; set; }
    public DateTime? FechaSalida { get; set; }
    public DateTime? FechaEntrega { get; set; }
}
