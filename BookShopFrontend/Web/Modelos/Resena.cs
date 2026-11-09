namespace Web.Modelos;

public class Resena
{
    public int Id
    {
        get; set;
    }
    public int IdLibro
    {
        get; set;
    }
    public string Usuario { get; set; } = "";
    public string FotoUsuario { get; set; } = "/images/user-placeholder.svg";
    public DateTime Fecha
    {
        get; set;
    }
    public int Puntuacion { get; set; } = 5;
    public string Comentario { get; set; } = "";
    public int Votos
    {
        get; set;
    }
    public bool Propia
    {
        get; set;
    }
    public bool Votada
    {
        get; set;
    }
    public bool Activo { get; set; } = true;
}
