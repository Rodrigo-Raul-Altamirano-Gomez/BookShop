using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class FiltroPedidoDTO
{
    [JsonProperty("estados")]
    public List<string> Estados { get; set; } = [];
    [JsonProperty("orden")]
    public string Orden { get; set; } = "FECHA_PAGO_DESCENDENTE";
    [JsonProperty("pagina")]
    public int Pagina { get; set; } = 1;
    [JsonProperty("tamanioPagina")]
    public int TamanioPagina { get; set; } = 10;
}
