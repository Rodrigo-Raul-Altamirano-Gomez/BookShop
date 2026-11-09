using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ConsultaResenasDTO
{
    [JsonProperty("orden")]
    public string Orden { get; set; } = "";

    [JsonProperty("pagina")]
    public int Pagina
    {
        get; set;
    }

    [JsonProperty("tamanioPagina")]
    public int TamanioPagina
    {
        get; set;
    }

    public ConsultaResenasDTO()
    {
    }

    public ConsultaResenasDTO(string orden, int pagina, int tamanioPagina)
    {
        Orden = orden;
        Pagina = pagina;
        TamanioPagina = tamanioPagina;
    }
}
