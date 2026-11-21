namespace BookShopWeb.Models;

public class Libro
{
    public int Id { get; set; }
    public string Isbn { get; set; } = "";
    public string Titulo { get; set; } = "";
    public string? Sinopsis { get; set; }
    public string Idioma { get; set; } = "";
    public int Paginas { get; set; }
    public int AnioPublicacion { get; set; }
    public decimal Precio { get; set; }
    public int Stock { get; set; }
    public string? Portada { get; set; }
    public bool Activo { get; set; }
    public DateTime FechaRegistro { get; set; }
    public Entidad Autor { get; set; } = new();
    public Entidad Categoria { get; set; } = new();
    public Entidad Editorial { get; set; } = new();
    public Descuento? DescuentoVigente { get; set; }
    public decimal PrecioFinal { get; set; }   // lo calcula el backend
    public double PuntuacionPromedio { get; set; }
    public int CantidadResenas { get; set; }
}
