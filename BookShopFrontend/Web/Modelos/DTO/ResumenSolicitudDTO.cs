using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ResumenSolicitudDTO
{
    [JsonProperty("id")]
    public int Id { get; set; }
    [JsonProperty("tipo")]
    public string Tipo { get; set; } = "";
    [JsonProperty("fechaSolicitud")]
    public DateTime FechaSolicitud { get; set; }
    [JsonProperty("estado")]
    public string Estado { get; set; } = "";
    [JsonProperty("nombreSolicitante")]
    public string NombreSolicitante { get; set; } = "";
    [JsonProperty("motivo")]
    public string Motivo { get; set; } = "";
    [JsonProperty("idReferencia")]
    public int IdReferencia { get; set; }
}
