namespace BookShopWeb.Models;

public class Descuento
{
    public int Id { get; set; }
    public Libro? Libro { get; set; }
    public int Porcentaje { get; set; }
    public DateOnly FechaInicio { get; set; }
    public DateOnly FechaFin { get; set; }
    public EstadoDescuento Estado { get; set; }
}
