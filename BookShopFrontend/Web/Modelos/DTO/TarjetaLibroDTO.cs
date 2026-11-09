using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class TarjetaLibroDTO
{
    [JsonProperty("id")]
    public int Id
    {
        get; set;
    }

    [JsonProperty("titulo")]
    public string Titulo { get; set; } = "";

    [JsonProperty("portada")]
    public ImagenDTO? Portada
    {
        get; set;
    }

    [JsonProperty("nombreAutor")]
    public string NombreAutor { get; set; } = "";

    [JsonProperty("precioOriginal")]
    public decimal PrecioOriginal
    {
        get; set;
    }

    [JsonProperty("precioConDescuento")]
    public decimal PrecioConDescuento
    {
        get; set;
    }

    [JsonProperty("porcentajeDescuento")]
    public decimal PorcentajeDescuento
    {
        get; set;
    }

    [JsonProperty("activo")]
    public bool Activo
    {
        get; set;
    }

    public TarjetaLibroDTO()
    {
    }

    public TarjetaLibroDTO(int id, string titulo, ImagenDTO? portada, string nombreAutor, decimal precioOriginal, decimal precioConDescuento, decimal porcentajeDescuento, bool activo)
    {
        Id = id;
        Titulo = titulo;
        Portada = portada;
        NombreAutor = nombreAutor;
        PrecioOriginal = precioOriginal;
        PrecioConDescuento = precioConDescuento;
        PorcentajeDescuento = porcentajeDescuento;
        Activo = activo;
    }
}
