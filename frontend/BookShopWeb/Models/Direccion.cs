namespace BookShopWeb.Models;

public class Direccion
{
    public int Id { get; set; }
    public Usuario Usuario { get; set; } = new();
    public string Alias { get; set; } = "";
    // En Java el atributo se llama "direccion"; aquí no puede llamarse igual que la clase
    [Newtonsoft.Json.JsonProperty("direccion")]
    public string Detalle { get; set; } = "";
    public string Distrito { get; set; } = "";
    public string Ciudad { get; set; } = "";
    public bool Predeterminada { get; set; }
    public bool Activa { get; set; }
}
