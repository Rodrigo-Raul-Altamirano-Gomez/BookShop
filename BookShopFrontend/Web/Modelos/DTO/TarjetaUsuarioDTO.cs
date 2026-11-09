using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class TarjetaUsuarioDTO
{
    [JsonProperty("id")]
    public int Id
    {
        get; set;
    }

    [JsonProperty("nombre")]
    public string Nombre { get; set; } = "";

    [JsonProperty("email")]
    public string Email { get; set; } = "";

    [JsonProperty("imagen")]
    public ImagenDTO? Imagen
    {
        get; set;
    }

    public TarjetaUsuarioDTO()
    {
    }

    public TarjetaUsuarioDTO(int id, string nombre, string email, ImagenDTO? imagen)
    {
        Id = id;
        Nombre = nombre;
        Email = email;
        Imagen = imagen;
    }
}
