using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DetalleLibroDTO
{
    [JsonProperty("libro")]
    public TarjetaLibroDTO Libro { get; set; } = null!;

    [JsonProperty("idAutor")]
    public int IdAutor { get; set; }

    [JsonProperty("idCategoria")]
    public int IdCategoria { get; set; }

    [JsonProperty("nombreCategoria")]
    public string NombreCategoria { get; set; } = "";

    [JsonProperty("idEditorial")]
    public int IdEditorial { get; set; }

    [JsonProperty("nombreEditorial")]
    public string NombreEditorial { get; set; } = "";

    [JsonProperty("idioma")]
    public string Idioma { get; set; } = "";

    [JsonProperty("sinopsis")]
    public string Sinopsis { get; set; } = "";

    [JsonProperty("isbn")]
    public string Isbn { get; set; } = "";

    [JsonProperty("anioEdicion")]
    public int AnioEdicion { get; set; }

    [JsonProperty("numeroPaginas")]
    public int NumeroPaginas { get; set; }

    [JsonProperty("peso")]
    public double? Peso { get; set; }

    [JsonProperty("alto")]
    public double? Alto { get; set; }

    [JsonProperty("ancho")]
    public double? Ancho { get; set; }

    [JsonProperty("saga")]
    public string Saga { get; set; } = "";

    [JsonProperty("numeroTomo")]
    public int? NumeroTomo { get; set; }

    [JsonProperty("stockDisponible")]
    public int StockDisponible { get; set; }

    [JsonProperty("valoracion")]
    public ValoracionLibroDTO Valoracion { get; set; } = null!;

    [JsonProperty("favorito")]
    public bool Favorito { get; set; }

    [JsonProperty("deseado")]
    public bool Deseado { get; set; }

    public DetalleLibroDTO()
    {
    }

    public DetalleLibroDTO(
        TarjetaLibroDTO libro, int idAutor, int idCategoria, string nombreCategoria, int idEditorial,
        string nombreEditorial, string idioma, string sinopsis, string isbn, int anioEdicion, int numeroPaginas,
        double? peso, double? alto, double? ancho, string saga, int? numeroTomo, int stockDisponible,
        ValoracionLibroDTO valoracion, bool favorito, bool deseado)
    {
        Libro = libro;
        IdAutor = idAutor;
        IdCategoria = idCategoria;
        NombreCategoria = nombreCategoria;
        IdEditorial = idEditorial;
        NombreEditorial = nombreEditorial;
        Idioma = idioma;
        Sinopsis = sinopsis;
        Isbn = isbn;
        AnioEdicion = anioEdicion;
        NumeroPaginas = numeroPaginas;
        Peso = peso;
        Alto = alto;
        Ancho = ancho;
        Saga = saga;
        NumeroTomo = numeroTomo;
        StockDisponible = stockDisponible;
        Valoracion = valoracion;
        Favorito = favorito;
        Deseado = deseado;
    }
}
