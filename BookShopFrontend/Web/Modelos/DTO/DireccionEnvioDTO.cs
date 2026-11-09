using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DireccionEnvioDTO
{
    [JsonProperty("id")]
    public int Id
    {
        get; set;
    }

    [JsonProperty("alias")]
    public string Alias { get; set; } = "";

    [JsonProperty("calle")]
    public string Calle { get; set; } = "";

    [JsonProperty("distrito")]
    public string Distrito { get; set; } = "";

    [JsonProperty("ciudad")]
    public string Ciudad { get; set; } = "";

    [JsonProperty("referencia")]
    public string Referencia { get; set; } = "";

    [JsonProperty("predeterminada")]
    public bool Predeterminada
    {
        get; set;
    }

    public DireccionEnvioDTO()
    {
    }

    public DireccionEnvioDTO(int id, string alias, string calle, string distrito, string ciudad, string referencia, bool predeterminada)
    {
        Id = id;
        Alias = alias;
        Calle = calle;
        Distrito = distrito;
        Ciudad = ciudad;
        Referencia = referencia;
        Predeterminada = predeterminada;
    }
}
