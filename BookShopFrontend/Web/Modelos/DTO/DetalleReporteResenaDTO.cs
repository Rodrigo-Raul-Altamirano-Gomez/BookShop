using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DetalleReporteResenaDTO
{
    [JsonProperty("solicitud")]
    public ResumenSolicitudDTO Solicitud { get; set; } = null!;
    [JsonProperty("datos")]
    public DatosReporteResenaDTO Datos { get; set; } = null!;
    [JsonProperty("resenaReportada")]
    public ResenaReportadaDTO ResenaReportada { get; set; } = null!;
    [JsonProperty("contenidoActual")]
    public DatosResenaDTO? ContenidoActual { get; set; }
    [JsonProperty("resenaActiva")]
    public bool ResenaActiva { get; set; }
    [JsonProperty("fechaResolucion")]
    public DateTime? FechaResolucion { get; set; }
}
