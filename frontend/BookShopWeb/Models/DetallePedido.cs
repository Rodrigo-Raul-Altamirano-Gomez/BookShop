namespace BookShopWeb.Models;

// Un libro del pedido, con el precio y el descuento del momento de la compra
public class DetallePedido
{
    public int Id { get; set; }
    public Libro Libro { get; set; } = new();
    public int Cantidad { get; set; }
    public decimal PrecioUnitario { get; set; }      // precio sin descuento
    public decimal DescuentoUnitario { get; set; }   // rebaja por unidad
    public decimal Subtotal { get; set; }
}
