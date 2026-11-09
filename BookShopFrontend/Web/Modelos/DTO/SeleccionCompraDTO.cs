using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class SeleccionCompraDTO
{
    [JsonProperty("idDireccionEnvio")]
    public int IdDireccionEnvio
    {
        get; set;
    }

    [JsonProperty("idMetodoPago")]
    public int IdMetodoPago
    {
        get; set;
    }

    [JsonProperty("idCupon")]
    public int IdCupon
    {
        get; set;
    }

    public SeleccionCompraDTO()
    {
    }

    public SeleccionCompraDTO(int idDireccionEnvio, int idMetodoPago, int idCupon)
    {
        IdDireccionEnvio = idDireccionEnvio;
        IdMetodoPago = idMetodoPago;
        IdCupon = idCupon;
    }
}
