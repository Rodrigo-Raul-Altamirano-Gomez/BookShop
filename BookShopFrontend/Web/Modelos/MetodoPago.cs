namespace Web.Modelos;

public class MetodoPago
{
    public string? Numero { get; set; }
    public bool Vigente { get; set; }
    public int Id { get; set; }
    public string Tipo { get; set; } = "Tarjeta";
    public string Alias { get; set; } = "";
    public string Titular { get; set; } = "";
    public string Proveedor { get; set; } = "";
    public string TipoTarjeta { get; set; } = "Débito";
    public string NumeroEnmascarado { get; set; } = "";
    public DateOnly Vencimiento { get; set; }
    public bool Predeterminada { get; set; }
}
