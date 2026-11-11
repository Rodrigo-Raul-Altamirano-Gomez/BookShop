using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ErrorRestDTO
{
    [JsonProperty("codigo")]
    public string Codigo { get; set; } = "";

    [JsonProperty("mensaje")]
    public string Mensaje { get; set; } = "";
}
