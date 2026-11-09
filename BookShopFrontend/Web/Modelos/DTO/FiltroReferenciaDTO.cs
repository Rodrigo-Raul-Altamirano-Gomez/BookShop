using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class FiltroReferenciaDTO
{
    [JsonProperty("nombre")]
    public string Nombre { get; set; } = "";

    [JsonProperty("soloActivos")]
    public bool SoloActivos
    {
        get; set;
    }

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

    public FiltroReferenciaDTO()
    {
    }

    public FiltroReferenciaDTO(string nombre, bool soloActivos, string orden, int pagina, int tamanioPagina)
    {
        Nombre = nombre;
        SoloActivos = soloActivos;
        Orden = orden;
        Pagina = pagina;
        TamanioPagina = tamanioPagina;
    }
}
