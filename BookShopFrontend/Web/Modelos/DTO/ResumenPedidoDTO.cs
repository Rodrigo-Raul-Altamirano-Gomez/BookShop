using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ResumenPedidoDTO
{
    [JsonProperty("id")]
    public int Id { get; set; }

    [JsonProperty("fechaPago")]
    public DateTime FechaPago { get; set; }

    [JsonProperty("estado")]
    public string Estado { get; set; } = "";

    [JsonProperty("cantidadLibros")]
    public int CantidadLibros { get; set; }

    [JsonProperty("total")]
    public decimal Total { get; set; }

    public ResumenPedidoDTO()
    {
    }

    public ResumenPedidoDTO(int id, DateTime fechaPago, string estado, int cantidadLibros, decimal total)
    {
        Id = id;
        FechaPago = fechaPago;
        Estado = estado;
        CantidadLibros = cantidadLibros;
        Total = total;
    }
}
