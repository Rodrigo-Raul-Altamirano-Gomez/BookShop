using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class PerfilUsuarioDTO
{
    [JsonProperty("usuario")]
    public TarjetaUsuarioDTO Usuario { get; set; } = null!;

    [JsonProperty("fechaNacimiento")]
    public DateOnly FechaNacimiento { get; set; }

    public PerfilUsuarioDTO()
    {
    }

    public PerfilUsuarioDTO(TarjetaUsuarioDTO usuario, DateOnly fechaNacimiento)
    {
        Usuario = usuario;
        FechaNacimiento = fechaNacimiento;
    }
}
