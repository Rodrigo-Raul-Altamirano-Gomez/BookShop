using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ImagenDTO
{
    [JsonProperty("contenido")]
    public int[] Contenido { get; set; } = [];

    [JsonProperty("tipoMime")]
    public string TipoMime { get; set; } = "";

    public ImagenDTO()
    {
    }

    public ImagenDTO(int[] contenido, string tipoMime)
    {
        Contenido = contenido;
        TipoMime = tipoMime;
    }
}
