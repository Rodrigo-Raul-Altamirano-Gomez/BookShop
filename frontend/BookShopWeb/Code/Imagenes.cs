using Microsoft.AspNetCore.Components.Forms;

namespace BookShopWeb.Code;

// Imágenes subidas desde los formularios (fotos, portadas, imágenes de entidades)
public static class Imagenes
{
    // Guarda el archivo en wwwroot/img/subidas y devuelve su ruta relativa, que es lo que se guarda en la base de datos
    public static async Task<string> Guardar(IBrowserFile archivo, string carpetaWeb)
    {
        string nombre = Guid.NewGuid().ToString("N") + Path.GetExtension(archivo.Name).ToLower();
        string carpeta = Path.Combine(carpetaWeb, "img", "subidas");
        Directory.CreateDirectory(carpeta);
        await using FileStream destino = File.Create(Path.Combine(carpeta, nombre));
        await archivo.OpenReadStream(5 * 1024 * 1024).CopyToAsync(destino);
        return "img/subidas/" + nombre;
    }
}
