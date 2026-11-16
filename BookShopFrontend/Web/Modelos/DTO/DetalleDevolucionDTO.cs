using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DetalleDevolucionDTO
{
    [JsonProperty("solicitud")]
    public ResumenSolicitudDTO Solicitud { get; set; } = null!;
    [JsonProperty("datos")]
    public DatosDevolucionDTO Datos { get; set; } = null!;
    [JsonProperty("pedido")]
    public ResumenPedidoDTO Pedido { get; set; } = null!;
    [JsonProperty("fechaResolucion")]
    public DateTime? FechaResolucion { get; set; }
}
