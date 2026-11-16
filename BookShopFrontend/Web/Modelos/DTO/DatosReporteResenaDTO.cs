using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DatosReporteResenaDTO
{
    [JsonProperty("motivo")]
    public string Motivo { get; set; } = "";
    [JsonProperty("comentario")]
    public string Comentario { get; set; } = "";
}
