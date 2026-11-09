using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ConsultaSeguimientosDTO
{
    [JsonProperty("tipo")]
    public string Tipo { get; set; } = "";

    [JsonProperty("orden")]
    public string Orden { get; set; } = "";

    [JsonProperty("pagina")]
    public int Pagina
    {
        get; set;
    }

    [JsonProperty("tamanioPagina")]
    public int TamanioPagina
    {
        get; set;
    }

    public ConsultaSeguimientosDTO()
    {
    }

    public ConsultaSeguimientosDTO(string tipo, string orden, int pagina, int tamanioPagina)
    {
        Tipo = tipo;
        Orden = orden;
        Pagina = pagina;
        TamanioPagina = tamanioPagina;
    }
}
