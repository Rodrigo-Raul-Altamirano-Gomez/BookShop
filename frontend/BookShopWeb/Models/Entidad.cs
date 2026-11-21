namespace BookShopWeb.Models;

// Autor, categoría o editorial
public class Entidad
{
    public int Id { get; set; }
    public TipoEntidad Tipo { get; set; }
    public string Nombre { get; set; } = "";
    public string? Descripcion { get; set; }   // biografía en el caso del autor
    public string? Imagen { get; set; }
    public bool Activo { get; set; }
}
