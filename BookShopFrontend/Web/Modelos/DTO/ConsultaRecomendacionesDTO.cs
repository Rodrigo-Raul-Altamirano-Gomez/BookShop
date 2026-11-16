using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ConsultaRecomendacionesDTO
{
    [JsonProperty("orden")]
    public string Orden { get; set; } = "RECOMENDADOS_PRIMERO";
    [JsonProperty("pagina")]
    public int Pagina { get; set; } = 1;
    [JsonProperty("tamanioPagina")]
    public int TamanioPagina { get; set; } = 12;
}
