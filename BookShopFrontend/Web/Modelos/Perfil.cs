namespace Web.Modelos;

public class Perfil
{
    public string Nombre { get; set; } = "";
    public string Correo { get; set; } = "";
    public DateOnly Nacimiento
    {
        get; set;
    }
    public string Imagen { get; set; } = "/images/user-placeholder.svg";
    public bool EsAdministrador
    {
        get; set;
    }
}
