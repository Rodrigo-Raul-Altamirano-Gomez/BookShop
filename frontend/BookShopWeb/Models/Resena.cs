namespace BookShopWeb.Models;

public class Resena
{
    public int Id { get; set; }
    public Usuario Usuario { get; set; } = new();
    public Libro Libro { get; set; } = new();
    public int Puntuacion { get; set; }
    public string Comentario { get; set; } = "";
    public DateTime Fecha { get; set; }
    public bool Activa { get; set; }         // false: oculta por moderación
    public int VotosUtiles { get; set; }
    public bool VotadaPorMi { get; set; }
}
