using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class TarjetaPedidoDTO
{
    [JsonProperty("pedido")]
    public ResumenPedidoDTO Pedido { get; set; } = new();
    [JsonProperty("portadas")]
    public List<string?> Portadas { get; set; } = [];
}
