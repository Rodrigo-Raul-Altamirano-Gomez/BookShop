using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class CuponDTO
{
    [JsonProperty("id")]
    public int Id
    {
        get; set;
    }

    [JsonProperty("porcentaje")]
    public decimal Porcentaje
    {
        get; set;
    }

    [JsonProperty("montoMinimo")]
    public decimal MontoMinimo
    {
        get; set;
    }

    [JsonProperty("fechaFin")]
    public DateTime FechaFin
    {
        get; set;
    }

    [JsonProperty("estado")]
    public string Estado { get; set; } = "";

    public CuponDTO()
    {
    }

    public CuponDTO(int id, decimal porcentaje, decimal montoMinimo, DateTime fechaFin, string estado)
    {
        Id = id;
        Porcentaje = porcentaje;
        MontoMinimo = montoMinimo;
        FechaFin = fechaFin;
        Estado = estado;
    }
}
