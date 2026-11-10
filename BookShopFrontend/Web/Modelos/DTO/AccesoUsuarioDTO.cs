using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class AccesoUsuarioDTO
{
    [JsonProperty("id")]
    public int Id { get; set; }

    [JsonProperty("nombre")]
    public string Nombre { get; set; } = "";

    [JsonProperty("rol")]
    public string Rol { get; set; } = "";

    [JsonProperty("requiereReactivacion")]
    public bool RequiereReactivacion { get; set; }

    public AccesoUsuarioDTO()
    {
    }

    public AccesoUsuarioDTO(int id, string nombre, string rol, bool requiereReactivacion)
    {
        Id = id;
        Nombre = nombre;
        Rol = rol;
        RequiereReactivacion = requiereReactivacion;
    }
}
