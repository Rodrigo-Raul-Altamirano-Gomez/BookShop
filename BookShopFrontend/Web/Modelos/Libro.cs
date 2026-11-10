namespace Web.Modelos;

public class Libro
{
    public int Id { get; set; }
    public string Titulo { get; set; } = "";
    public string Autor { get; set; } = "";
    public string Categoria { get; set; } = "";
    public string Editorial { get; set; } = "";
    public string Idioma { get; set; } = "Español";
    public string Sinopsis { get; set; } = "";
    public string ISBN { get; set; } = "";
    public decimal Precio { get; set; }
    public decimal PrecioOriginal { get; set; }
    public decimal Descuento { get; set; }
    public int Stock { get; set; } = 0;
    public int Paginas { get; set; } = 0;
    public int Anio { get; set; } = 0;
    public string Peso { get; set; } = "—";
    public string Dimensiones { get; set; } = "—";
    public string Saga { get; set; } = "";
    public int? Tomo { get; set; }
    public string Portada { get; set; } = "/images/book-placeholder.svg";
    public bool Activo { get; set; } = true;

}
