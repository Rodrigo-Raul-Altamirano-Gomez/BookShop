namespace BookShopWeb.Models;

public class Cupon
{
    public int Id { get; set; }
    public string Codigo { get; set; } = "";
    public int Porcentaje { get; set; }
    public DateOnly FechaEmision { get; set; }
    public DateOnly FechaVencimiento { get; set; }
    public bool Activo { get; set; }
    public bool Utilizable { get; set; }   // lo calcula el backend
    public Pedido? Pedido { get; set; }    // pedido en que se usó
}
