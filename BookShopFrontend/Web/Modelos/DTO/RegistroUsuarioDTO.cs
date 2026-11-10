using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class RegistroUsuarioDTO
{
    [JsonProperty("nombre")]
    public string Nombre { get; set; } = "";

    [JsonProperty("email")]
    public string Email { get; set; } = "";

    [JsonProperty("fechaNacimiento")]
    public DateOnly FechaNacimiento { get; set; }

    [JsonProperty("contrasenia")]
    public string Contrasenia { get; set; } = "";

    [JsonProperty("confirmacionContrasenia")]
    public string ConfirmacionContrasenia { get; set; } = "";

    [JsonProperty("imagen")]
    public string? Imagen { get; set; }

    public RegistroUsuarioDTO()
    {
    }

    public RegistroUsuarioDTO(string nombre, string email, DateOnly fechaNacimiento, string contrasenia, string confirmacionContrasenia, string? imagen)
    {
        Nombre = nombre;
        Email = email;
        FechaNacimiento = fechaNacimiento;
        Contrasenia = contrasenia;
        ConfirmacionContrasenia = confirmacionContrasenia;
        Imagen = imagen;
    }
}
