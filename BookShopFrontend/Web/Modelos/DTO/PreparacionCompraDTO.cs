using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class PreparacionCompraDTO
{
    [JsonProperty("resumen")]
    public ResumenCompraDTO Resumen { get; set; } = null!;

    [JsonProperty("seleccion")]
    public SeleccionCompraDTO Seleccion { get; set; } = null!;

    [JsonProperty("puedePagar")]
    public bool PuedePagar { get; set; }

    public PreparacionCompraDTO()
    {
    }

    public PreparacionCompraDTO(ResumenCompraDTO resumen, SeleccionCompraDTO seleccion, bool puedePagar)
    {
        Resumen = resumen;
        Seleccion = seleccion;
        PuedePagar = puedePagar;
    }
}
