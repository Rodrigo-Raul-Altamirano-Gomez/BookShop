using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ResultadoPaginadoDTO<T>
{
    [JsonProperty("items")]
    public List<T> Items { get; set; } = [];

    [JsonProperty("totalResultados")]
    public long TotalResultados { get; set; }

    [JsonProperty("pagina")]
    public int Pagina { get; set; }

    [JsonProperty("tamanioPagina")]
    public int TamanioPagina { get; set; }

    public ResultadoPaginadoDTO()
    {
    }

    public ResultadoPaginadoDTO(List<T> items, long totalResultados, int pagina, int tamanioPagina)
    {
        Items = items;
        TotalResultados = totalResultados;
        Pagina = pagina;
        TamanioPagina = tamanioPagina;
    }
}
