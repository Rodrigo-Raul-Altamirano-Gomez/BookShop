using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ImagenDTO
{
    [JsonProperty("url")]
    public string Url { get; set; } = "";

    public ImagenDTO()
    {
    }
    public ImagenDTO(string url)
    {
        Url = url;
    }
}
