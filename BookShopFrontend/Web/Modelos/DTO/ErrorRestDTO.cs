using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ErrorRestDTO
{
    [JsonProperty("mensaje")]
    public string Mensaje { get; set; } = "";
}
