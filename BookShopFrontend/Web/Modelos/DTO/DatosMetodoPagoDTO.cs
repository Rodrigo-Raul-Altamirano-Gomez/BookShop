using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class DatosMetodoPagoDTO
{
    [JsonProperty("tarjetaBancaria")]
    public bool TarjetaBancaria
    {
        get; set;
    }

    [JsonProperty("titular")]
    public string Titular { get; set; } = "";

    [JsonProperty("proveedor")]
    public string Proveedor { get; set; } = "";

    [JsonProperty("alias")]
    public string Alias { get; set; } = "";

    [JsonProperty("numero")]
    public string? Numero
    {
        get; set;
    }

    [JsonProperty("tipoTarjeta")]
    public string? TipoTarjeta
    {
        get; set;
    }

    [JsonProperty("fechaVencimiento")]
    public DateOnly? FechaVencimiento
    {
        get; set;
    }

    [JsonProperty("predeterminado")]
    public bool Predeterminado
    {
        get; set;
    }

    public DatosMetodoPagoDTO()
    {
    }

    public DatosMetodoPagoDTO(bool tarjetaBancaria, string titular, string proveedor, string alias, string? numero, string? tipoTarjeta, DateOnly? fechaVencimiento, bool predeterminado)
    {
        TarjetaBancaria = tarjetaBancaria;
        Titular = titular;
        Proveedor = proveedor;
        Alias = alias;
        Numero = numero;
        TipoTarjeta = tipoTarjeta;
        FechaVencimiento = fechaVencimiento;
        Predeterminado = predeterminado;
    }
}
