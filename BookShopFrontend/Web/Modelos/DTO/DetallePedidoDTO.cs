using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DetallePedidoDTO
{
    [JsonProperty("pedido")]
    public ResumenPedidoDTO Pedido { get; set; } = null!;

    [JsonProperty("resumen")]
    public ResumenCompraDTO Resumen { get; set; } = null!;

    [JsonProperty("direccionEnvio")]
    public DireccionEnvioDTO DireccionEnvio { get; set; } = null!;

    [JsonProperty("metodoPago")]
    public MetodoPagoDTO MetodoPago { get; set; } = null!;

    [JsonProperty("identificadorTransaccion")]
    public string IdentificadorTransaccion { get; set; } = "";

    [JsonProperty("codigoSeguimiento")]
    public string CodigoSeguimiento { get; set; } = "";

    [JsonProperty("fechaEntregaEstimada")]
    public DateTime? FechaEntregaEstimada
    {
        get; set;
    }

    [JsonProperty("fechaSalida")]
    public DateTime? FechaSalida
    {
        get; set;
    }

    [JsonProperty("fechaEntrega")]
    public DateTime? FechaEntrega
    {
        get; set;
    }

    [JsonProperty("puedeCancelar")]
    public bool PuedeCancelar
    {
        get; set;
    }

    [JsonProperty("puedeSolicitarDevolucion")]
    public bool PuedeSolicitarDevolucion
    {
        get; set;
    }

    public DetallePedidoDTO()
    {
    }

    public DetallePedidoDTO(ResumenPedidoDTO pedido, ResumenCompraDTO resumen, DireccionEnvioDTO direccionEnvio, MetodoPagoDTO metodoPago, string identificadorTransaccion, string codigoSeguimiento, DateTime? fechaEntregaEstimada, DateTime? fechaSalida, DateTime? fechaEntrega, bool puedeCancelar, bool puedeSolicitarDevolucion)
    {
        Pedido = pedido;
        Resumen = resumen;
        DireccionEnvio = direccionEnvio;
        MetodoPago = metodoPago;
        IdentificadorTransaccion = identificadorTransaccion;
        CodigoSeguimiento = codigoSeguimiento;
        FechaEntregaEstimada = fechaEntregaEstimada;
        FechaSalida = fechaSalida;
        FechaEntrega = fechaEntrega;
        PuedeCancelar = puedeCancelar;
        PuedeSolicitarDevolucion = puedeSolicitarDevolucion;
    }
}
