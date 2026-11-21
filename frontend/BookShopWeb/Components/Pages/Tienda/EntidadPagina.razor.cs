using BookShopWeb.Code;
using BookShopWeb.Models;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Pages.Tienda;

// Página de un autor, una categoría o una editorial (UX-UI §8.6)
public partial class EntidadPagina
{
    [Parameter] public int Id { get; set; }

    private Entidad? entidad;
    private bool noEncontrado;

    protected override async Task OnParametersSetAsync()
    {
        if (entidad?.Id == Id) return;
        entidad = null;
        noEncontrado = false;
        try
        {
            entidad = await new HttpClientUtils<Entidad>().Get($"entidades/{Id}");
        }
        catch (ApiException ex) when (ex.Estado == 404)
        {
            noEncontrado = true;
        }
    }
}
