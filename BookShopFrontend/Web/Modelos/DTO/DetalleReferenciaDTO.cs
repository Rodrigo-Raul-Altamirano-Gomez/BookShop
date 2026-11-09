using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DetalleReferenciaDTO
{
    [JsonProperty("referencia")]
    public TarjetaReferenciaDTO Referencia { get; set; } = null!;

    [JsonProperty("introduccion")]
    public string Introduccion { get; set; } = "";

    [JsonProperty("activo")]
    public bool Activo
    {
        get; set;
    }

    [JsonProperty("seguido")]
    public bool Seguido
    {
        get; set;
    }

    public DetalleReferenciaDTO()
    {
    }

    public DetalleReferenciaDTO(TarjetaReferenciaDTO referencia, string introduccion, bool activo, bool seguido)
    {
        Referencia = referencia;
        Introduccion = introduccion;
        Activo = activo;
        Seguido = seguido;
    }
}
