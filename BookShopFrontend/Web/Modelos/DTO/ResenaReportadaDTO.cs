using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ResenaReportadaDTO
{
    [JsonProperty("idResena")]
    public int IdResena { get; set; }
    [JsonProperty("nombreUsuario")]
    public string NombreUsuario { get; set; } = "";
    [JsonProperty("tituloLibro")]
    public string TituloLibro { get; set; } = "";
    [JsonProperty("contenidoOriginal")]
    public DatosResenaDTO ContenidoOriginal { get; set; } = null!;
}
