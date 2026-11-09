using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class FiltroLibroDTO
{
    [JsonProperty("titulo")]
    public string? Titulo
    {
        get; set;
    }

    [JsonProperty("idsCategorias")]
    public List<int> IdsCategorias { get; set; } = [];

    [JsonProperty("idsAutores")]
    public List<int> IdsAutores { get; set; } = [];

    [JsonProperty("idsEditoriales")]
    public List<int> IdsEditoriales { get; set; } = [];

    [JsonProperty("idiomas")]
    public List<string> Idiomas { get; set; } = [];

    [JsonProperty("precioMinimo")]
    public decimal? PrecioMinimo
    {
        get; set;
    }

    [JsonProperty("precioMaximo")]
    public decimal? PrecioMaximo
    {
        get; set;
    }

    [JsonProperty("soloActivos")]
    public bool SoloActivos
    {
        get; set;
    }

    [JsonProperty("soloNuevos")]
    public bool SoloNuevos
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

    public FiltroLibroDTO()
    {
    }

    public FiltroLibroDTO(string? titulo, List<int> idsCategorias, List<int> idsAutores, List<int> idsEditoriales, List<string> idiomas, decimal? precioMinimo, decimal? precioMaximo, bool soloActivos, bool soloNuevos, string orden, int pagina, int tamanioPagina)
    {
        Titulo = titulo;
        IdsCategorias = idsCategorias;
        IdsAutores = idsAutores;
        IdsEditoriales = idsEditoriales;
        Idiomas = idiomas;
        PrecioMinimo = precioMinimo;
        PrecioMaximo = precioMaximo;
        SoloActivos = soloActivos;
        SoloNuevos = soloNuevos;
        Orden = orden;
        Pagina = pagina;
        TamanioPagina = tamanioPagina;
    }
}
