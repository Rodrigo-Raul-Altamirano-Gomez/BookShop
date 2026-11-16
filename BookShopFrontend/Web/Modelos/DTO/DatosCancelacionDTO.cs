using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DatosCancelacionDTO
{
    [JsonProperty("motivo")]
    public string Motivo { get; set; } = "";
}
