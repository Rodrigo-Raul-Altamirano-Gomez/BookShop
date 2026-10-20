using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Microsoft.AspNetCore.Components.Web;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Admin;

public partial class RegistroLibro
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = null!;

    [Parameter]
    public EventCallback AlCerrar
    {
        get; set;
    }

    private readonly string[] TiposReferencias = ["autores", "categorias", "editoriales"];
    private readonly Dictionary<string, string> Busquedas = new()
    {

        ["autores"] = "",

        ["categorias"] = "",

        ["editoriales"] = ""

    };
    private readonly Dictionary<string, string> BusquedasAplicadas = new()
    {

        ["autores"] = "",

        ["categorias"] = "",

        ["editoriales"] = ""

    };
    private readonly Dictionary<string, string> Selecciones = new()
    {

        ["autores"] = "",

        ["categorias"] = "",

        ["editoriales"] = ""

    };
    private Libro Borrador = new()
    {

        Anio = 2026,

        Paginas = 1,

        Stock = 0,

        Peso = "",

        Dimensiones = "",

        Proveedor = ""

    };
    private string ErrorFormulario = "", Peso = "", Alto = "", Ancho = "";
    private string EtiquetaReferencia(string Tipo) => Tipo switch
    {
        "autores" => "Autor",
        "categorias" => "Categoría",
        _ => "Editorial"
    };
    private IEnumerable<Referencia> Opciones(string Tipo) => Estado.Referencias.Where(r => r.Tipo == Tipo
        && r.Activo && r.Nombre.Contains(BusquedasAplicadas[Tipo], StringComparison.OrdinalIgnoreCase))
            .OrderBy(r => r.Nombre).Take(5);
    private void CambiarBusqueda(string Tipo, ChangeEventArgs Argumentos) => Busquedas[Tipo] = Argumentos
        .Value?.ToString() ?? "";
    private void Buscar(string Tipo) => BusquedasAplicadas[Tipo] = Busquedas[Tipo].Trim();
    private void BuscarConEnter(string Tipo, KeyboardEventArgs Argumentos)
    {
        if (Argumentos.Key != "Enter")
            return;
        Buscar(Tipo);
    }

    private void Elegir(Referencia Referencia) => Selecciones[Referencia.Tipo] = Referencia.Nombre;
    private void QuitarPortada() => Borrador.Portada = "/images/book-placeholder.svg";
    private async Task LeerPortada(InputFileChangeEventArgs Argumentos)
    {
        ErrorFormulario = "";
        if (Argumentos.File.ContentType is not ("image/jpeg" or "image/png") || Argumentos.File
            .Size > 5 * 1024 * 1024)
        {
            ErrorFormulario = "Selecciona una portada JPEG o PNG de hasta 5 MiB.";
            return;
        }

        try
        {
            using var Flujo = Argumentos.File.OpenReadStream(5 * 1024 * 1024);
            using var Memoria = new MemoryStream();
            await Flujo.CopyToAsync(Memoria);
            Borrador.Portada = ($"data:" + $"{Argumentos.File.ContentType};base64," + $"{Convert
                .ToBase64String(Memoria.ToArray())}");
        }
        catch (IOException)
        {
            ErrorFormulario = "No se pudo leer la portada. Prueba con otro archivo.";
        }
    }

    private async Task Registrar()
    {
        if (string.IsNullOrWhiteSpace(Borrador.Titulo) || string.IsNullOrWhiteSpace(Borrador.Sinopsis)
            || string.IsNullOrWhiteSpace(Borrador.Idioma) || string.IsNullOrWhiteSpace(Borrador.ISBN)
                || string.IsNullOrWhiteSpace(Borrador.Proveedor) || Selecciones.Values.Any(string
                    .IsNullOrWhiteSpace))
        {
            ErrorFormulario = "Completa los campos obligatorios y selecciona autor, categoría y editorial.";
            return;
        }

        if (Borrador.PrecioOriginal <= 0 || Borrador.Stock < 0 || Borrador.Paginas < 1 || Borrador.Anio < 1)
        {
            ErrorFormulario = "Introduce valores válidos para precio, stock, páginas y año.";
            return;
        }

        Borrador.Id = Estado.Libros.Select(b => b.Id).DefaultIfEmpty(0).Max() + 1;
        Borrador.Autor = Selecciones["autores"];
        Borrador.Categoria = Selecciones["categorias"];
        Borrador.Editorial = Selecciones["editoriales"];
        Borrador.Precio = Borrador.PrecioOriginal;
        Borrador.Peso = string.IsNullOrWhiteSpace(Peso) ? "" : Peso + " kg";
        Borrador.Dimensiones = string.IsNullOrWhiteSpace(Alto) && string.IsNullOrWhiteSpace(Ancho) ? ""
            : Alto + " × " + Ancho + " cm";
        Estado.Libros.Add(Borrador);
        Estado.Notificar();
        Estado.Avisar("Libro registrado");
        await AlCerrar.InvokeAsync();
    }
}

