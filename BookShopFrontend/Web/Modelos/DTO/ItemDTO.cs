using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ItemDTO
{
    [JsonProperty("idLibro")]
    public int IdLibro
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

    [JsonProperty("cantidad")]
    public int Cantidad
    {
        get; set;
    }

    [JsonProperty("precioOriginal")]
    public decimal PrecioOriginal
    {
        get; set;
    }

    [JsonProperty("precioUnitario")]
    public decimal PrecioUnitario
    {
        get; set;
    }

    [JsonProperty("porcentajeDescuento")]
    public decimal PorcentajeDescuento
    {
        get; set;
    }

    [JsonProperty("subtotal")]
    public decimal Subtotal
    {
        get; set;
    }

    [JsonProperty("activo")]
    public bool? Activo
    {
        get; set;
    }

    [JsonProperty("stockDisponible")]
    public int? StockDisponible
    {
        get; set;
    }

    public ItemDTO()
    {
    }

    public ItemDTO(int idLibro, string titulo, ImagenDTO? portada, int cantidad, decimal precioOriginal, decimal precioUnitario, decimal porcentajeDescuento, decimal subtotal, bool? activo, int? stockDisponible)
    {
        IdLibro = idLibro;
        Titulo = titulo;
        Portada = portada;
        Cantidad = cantidad;
        PrecioOriginal = precioOriginal;
        PrecioUnitario = precioUnitario;
        PorcentajeDescuento = porcentajeDescuento;
        Subtotal = subtotal;
        Activo = activo;
        StockDisponible = stockDisponible;
    }
}
