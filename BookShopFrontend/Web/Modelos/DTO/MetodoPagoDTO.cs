using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class MetodoPagoDTO
{
    [JsonProperty("id")]
    public int Id { get; set; }

    [JsonProperty("tarjetaBancaria")]
    public bool TarjetaBancaria { get; set; }

    [JsonProperty("titular")]
    public string Titular { get; set; } = "";

    [JsonProperty("proveedor")]
    public string Proveedor { get; set; } = "";

    [JsonProperty("alias")]
    public string Alias { get; set; } = "";

    [JsonProperty("numeroEnmascarado")]
    public string NumeroEnmascarado { get; set; } = "";

    [JsonProperty("tipoTarjeta")]
    public string? TipoTarjeta { get; set; }

    [JsonProperty("fechaVencimiento")]
    public DateOnly FechaVencimiento { get; set; }

    [JsonProperty("predeterminado")]
    public bool Predeterminado { get; set; }

    [JsonProperty("vigente")]
    public bool Vigente { get; set; }

    public MetodoPagoDTO()
    {
    }

    public MetodoPagoDTO(
        int id, bool tarjetaBancaria, string titular, string proveedor, string alias, string numeroEnmascarado,
        string? tipoTarjeta, DateOnly fechaVencimiento, bool predeterminado, bool vigente)
    {
        Id = id;
        TarjetaBancaria = tarjetaBancaria;
        Titular = titular;
        Proveedor = proveedor;
        Alias = alias;
        NumeroEnmascarado = numeroEnmascarado;
        TipoTarjeta = tipoTarjeta;
        FechaVencimiento = fechaVencimiento;
        Predeterminado = predeterminado;
        Vigente = vigente;
    }
}
