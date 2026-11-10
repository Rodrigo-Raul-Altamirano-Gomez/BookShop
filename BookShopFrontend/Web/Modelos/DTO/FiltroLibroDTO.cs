using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class FiltroLibroDTO
{
    [JsonProperty("titulo")]
    public string? Titulo { get; set; }

    [JsonProperty("idsCategorias")]
    public List<int> IdsCategorias { get; set; } = [];

    [JsonProperty("idsAutores")]
    public List<int> IdsAutores { get; set; } = [];

    [JsonProperty("idsEditoriales")]
    public List<int> IdsEditoriales { get; set; } = [];

    [JsonProperty("idiomas")]
    public List<string> Idiomas { get; set; } = [];

    [JsonProperty("precioMinimo")]
    public decimal? PrecioMinimo { get; set; }

    [JsonProperty("precioMaximo")]
    public decimal? PrecioMaximo { get; set; }

    [JsonProperty("soloActivos")]
    public bool SoloActivos { get; set; } = true;

    [JsonProperty("soloNuevos")]
    public bool SoloNuevos { get; set; }

    [JsonProperty("orden")]
    public string Orden { get; set; } = "MAS_RECIENTES";

    [JsonProperty("pagina")]
    public int Pagina { get; set; } = 1;

    [JsonProperty("tamanioPagina")]
    public int TamanioPagina { get; set; } = 12;

}
