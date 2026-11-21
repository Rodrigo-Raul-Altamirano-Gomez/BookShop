using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Pages.Tienda;

public partial class LibroDetalle
{
    [Parameter] public int Id { get; set; }
    [Inject] private ComparacionService Comparacion { get; set; } = default!;

    private Libro? libro;
    private bool noEncontrado;

    // Al pasar de un libro a otro (enlaces del detalle), se vuelve a cargar
    protected override async Task OnParametersSetAsync()
    {
        libro = null;
        noEncontrado = false;
        try
        {
            libro = await new HttpClientUtils<Libro>().Get($"libros/{Id}");
        }
        catch (ApiException ex) when (ex.Estado == 404)
        {
            noEncontrado = true;
        }
    }

    // RF09.7: cualquier usuario puede comparar
    private async Task Comparar()
    {
        await Comparacion.Agregar(libro!);
    }
}
