namespace BookShopWeb.Models;

public class Usuario
{
    public int Id { get; set; }
    public string Correo { get; set; } = "";
    public string NombreCompleto { get; set; } = "";
    public DateOnly FechaNacimiento { get; set; }
    public string? Contrasena { get; set; }
    public string? Foto { get; set; }
    public Rol Rol { get; set; }
    public bool Activo { get; set; }
    public DateTime FechaRegistro { get; set; }
}
