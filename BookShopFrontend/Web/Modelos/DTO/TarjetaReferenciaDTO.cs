using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class TarjetaReferenciaDTO
{
    [JsonProperty("id")]
    public int Id
    {
        get; set;
    }

    [JsonProperty("nombre")]
    public string Nombre { get; set; } = "";

    [JsonProperty("imagen")]
    public ImagenDTO? Imagen
    {
        get; set;
    }

    public TarjetaReferenciaDTO()
    {
    }

    public TarjetaReferenciaDTO(int id, string nombre, ImagenDTO? imagen)
    {
        Id = id;
        Nombre = nombre;
        Imagen = imagen;
    }
}
