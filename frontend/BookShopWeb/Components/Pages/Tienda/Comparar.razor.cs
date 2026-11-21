using System.Globalization;
using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Pages.Tienda;

public partial class Comparar : IDisposable
{
    [Inject] private ComparacionService Comparacion { get; set; } = default!;

    private List<Libro> libros = new();
    private bool cargando = true;

    protected override async Task OnInitializedAsync()
    {
        await Comparacion.Cargar();
        await CargarLibros();
        // Después de la primera carga: al quitar un libro o elegir otro, se vuelve a cargar
        Comparacion.Cambio += AlCambiar;
    }

    private async Task CargarLibros()
    {
        // Los dos libros se piden a la vez
        List<Task<Libro?>> pedidos = new();
        foreach (LibroComparado elegido in Comparacion.Libros)
        {
            pedidos.Add(new HttpClientUtils<Libro>().Get($"libros/{elegido.Id}"));
        }
        libros = (await Task.WhenAll(pedidos)).Select(l => l!).ToList();
        cargando = false;
    }

    private async Task Quitar(int id)
    {
        await Comparacion.Quitar(id);
    }

    private async void AlCambiar()
    {
        await InvokeAsync(async () =>
        {
            await CargarLibros();
            StateHasChanged();
        });
    }

    // RF09.8: atributos que se muestran como texto, lado a lado
    private static List<(string Atributo, string ValorX, string ValorY)> FilasDeTexto(Libro x, Libro y) => new()
    {
        ("Autor", x.Autor.Nombre, y.Autor.Nombre),
        ("Categoría", x.Categoria.Nombre, y.Categoria.Nombre),
        ("Editorial", x.Editorial.Nombre, y.Editorial.Nombre),
        ("Idioma", x.Idioma, y.Idioma),
        ("Páginas", x.Paginas.ToString(), y.Paginas.ToString()),
        ("Año", x.AnioPublicacion.ToString(), y.AnioPublicacion.ToString())
    };

    // Las filas donde los libros difieren se marcan con verde suave (UX-UI §8.4)
    private static string Diferencia(string a, string b) => a != b ? "background:var(--bk-green-50)" : "";

    private static string Promedio(Libro libro) => libro.PuntuacionPromedio.ToString("0.0", CultureInfo.InvariantCulture);

    private static string Disponibilidad(Libro libro) => libro.Stock > 0 ? "En stock" : "Agotado";

    public void Dispose()
    {
        Comparacion.Cambio -= AlCambiar;
    }
}
