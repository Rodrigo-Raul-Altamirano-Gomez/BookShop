using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DatosResenaDTO
{
    [JsonProperty("puntuacion")]
    public int Puntuacion { get; set; } = 5;
    [JsonProperty("comentario")]
    public string Comentario { get; set; } = "";
}
