using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DatosPerfilDTO
{
    [JsonProperty("nombre")]
    public string Nombre { get; set; } = "";

    [JsonProperty("fechaNacimiento")]
    public DateOnly FechaNacimiento { get; set; }

    public DatosPerfilDTO()
    {
    }

    public DatosPerfilDTO(string nombre, DateOnly fechaNacimiento)
    {
        Nombre = nombre;
        FechaNacimiento = fechaNacimiento;
    }
}
