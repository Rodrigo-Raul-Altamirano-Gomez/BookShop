namespace Web.Modelos;

public class Referencia
{
    public int Id
    {
        get; set;
    }
    public string Tipo { get; set; } = "categorias";
    public string Nombre { get; set; } = "";
    public string Descripcion { get; set; } = "";
    public string Imagen { get; set; } = "/images/reference-placeholder.svg";
    public bool Activo { get; set; } = true;
}
