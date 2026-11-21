namespace BookShopWeb.Models;

public class MetodoPago
{
    public int Id { get; set; }
    public Usuario Usuario { get; set; } = new();
    public TipoMetodoPago Tipo { get; set; }
    public string Titular { get; set; } = "";
    public string Numero { get; set; } = "";
    public string? FechaVencimiento { get; set; }   // MM/AA, solo tarjeta
    public string? Proveedor { get; set; }          // Yape o Plin, solo billetera
    public bool Predeterminado { get; set; }
    public bool Activo { get; set; }
}
