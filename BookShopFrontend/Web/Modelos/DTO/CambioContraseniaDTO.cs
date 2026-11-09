using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class CambioContraseniaDTO
{
    [JsonProperty("contraseniaActual")]
    public string ContraseniaActual { get; set; } = "";

    [JsonProperty("contraseniaNueva")]
    public string ContraseniaNueva { get; set; } = "";

    [JsonProperty("confirmacionContrasenia")]
    public string ConfirmacionContrasenia { get; set; } = "";

    public CambioContraseniaDTO()
    {
    }

    public CambioContraseniaDTO(string contraseniaActual, string contraseniaNueva, string confirmacionContrasenia)
    {
        ContraseniaActual = contraseniaActual;
        ContraseniaNueva = contraseniaNueva;
        ConfirmacionContrasenia = confirmacionContrasenia;
    }
}
