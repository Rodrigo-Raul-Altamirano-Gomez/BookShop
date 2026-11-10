namespace Web.Modelos;

public class Direccion
{
    public int Id { get; set; }
    public string Alias { get; set; } = "";
    public string Calle { get; set; } = "";
    public string Distrito { get; set; } = "";
    public string Ciudad { get; set; } = "Lima";
    public string ReferenciaAdicional { get; set; } = "";
    public bool Predeterminada { get; set; }
}
