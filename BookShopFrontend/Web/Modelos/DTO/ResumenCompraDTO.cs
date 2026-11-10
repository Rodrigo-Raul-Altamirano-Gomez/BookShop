using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ResumenCompraDTO
{
    [JsonProperty("items")]
    public List<ItemDTO> Items { get; set; } = [];

    [JsonProperty("subtotalProductos")]
    public decimal SubtotalProductos { get; set; }

    [JsonProperty("descuentoLibros")]
    public decimal DescuentoLibros { get; set; }

    [JsonProperty("idCupon")]
    public int IdCupon { get; set; }

    [JsonProperty("porcentajeCupon")]
    public decimal PorcentajeCupon { get; set; }

    [JsonProperty("descuentoCupon")]
    public decimal DescuentoCupon { get; set; }

    [JsonProperty("porcentajeEnvio")]
    public decimal PorcentajeEnvio { get; set; }

    [JsonProperty("costoEnvio")]
    public decimal CostoEnvio { get; set; }

    [JsonProperty("total")]
    public decimal Total { get; set; }

    [JsonProperty("cantidadLibros")]
    public int CantidadLibros { get; set; }

    public ResumenCompraDTO()
    {
    }

    public ResumenCompraDTO(List<ItemDTO> items, decimal subtotalProductos, decimal descuentoLibros, int idCupon, decimal porcentajeCupon, decimal descuentoCupon, decimal porcentajeEnvio, decimal costoEnvio, decimal total, int cantidadLibros)
    {
        Items = items;
        SubtotalProductos = subtotalProductos;
        DescuentoLibros = descuentoLibros;
        IdCupon = idCupon;
        PorcentajeCupon = porcentajeCupon;
        DescuentoCupon = descuentoCupon;
        PorcentajeEnvio = porcentajeEnvio;
        CostoEnvio = costoEnvio;
        Total = total;
        CantidadLibros = cantidadLibros;
    }
}
