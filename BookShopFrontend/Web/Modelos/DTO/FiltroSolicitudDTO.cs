using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class FiltroSolicitudDTO
{
    [JsonProperty("tipo")]
    public string Tipo { get; set; } = "DEVOLUCION";
    [JsonProperty("estados")]
    public List<string> Estados { get; set; } = [];
    [JsonProperty("pagina")]
    public int Pagina { get; set; } = 1;
    [JsonProperty("tamanioPagina")]
    public int TamanioPagina { get; set; } = 10;
}
