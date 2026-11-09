namespace Web.Modelos;

public class Cupon
{
    public int Id
    {
        get; set;
    }
    public decimal Porcentaje
    {
        get; set;
    }
    public decimal Minimo { get; set; } = 100;
    public DateOnly Vencimiento
    {
        get; set;
    }
    public string Estado { get; set; } = "Disponible";
}
