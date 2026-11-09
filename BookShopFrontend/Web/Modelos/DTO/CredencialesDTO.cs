using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class CredencialesDTO
{
    [JsonProperty("email")]
    public string Email { get; set; } = "";

    [JsonProperty("contrasenia")]
    public string Contrasenia { get; set; } = "";

    public CredencialesDTO()
    {
    }

    public CredencialesDTO(string email, string contrasenia)
    {
        Email = email;
        Contrasenia = contrasenia;
    }
}
