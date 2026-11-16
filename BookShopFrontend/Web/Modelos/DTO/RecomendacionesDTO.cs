using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class RecomendacionesDTO
{
    [JsonProperty("idConsulta")]
    public string IdConsulta { get; set; } = "";
    [JsonProperty("resultados")]
    public ResultadoPaginadoDTO<TarjetaLibroDTO> Resultados { get; set; } = new();
}
