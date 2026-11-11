namespace Web.Modelos;

public class LineaPedido
{
    public int IdLibro { get; set; }
    public string Titulo { get; set; } = "";
    public string? Portada { get; set; }
    public int Cantidad { get; set; }
    public decimal PrecioOriginal { get; set; }
    public decimal Precio { get; set; }
    public decimal Descuento { get; set; }
}
