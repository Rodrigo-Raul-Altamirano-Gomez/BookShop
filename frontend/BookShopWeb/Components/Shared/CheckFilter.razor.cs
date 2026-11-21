using BookShopWeb.Models;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Shared;

public partial class CheckFilter
{
    [Parameter, EditorRequired] public string Clave { get; set; } = "";
    [Parameter, EditorRequired] public string Titulo { get; set; } = "";
    [Parameter, EditorRequired] public string Plural { get; set; } = "";
    [Parameter] public List<Entidad> Opciones { get; set; } = new();
    [Parameter] public List<int> Seleccionadas { get; set; } = new();
    [Parameter] public EventCallback<List<int>> OnCambio { get; set; }

    private bool abierto;
    private string busqueda = "";
    private bool buscando => busqueda.Trim() != "";

    private async Task Alternar(int id)
    {
        List<int> nuevas = new List<int>(Seleccionadas);
        if (!nuevas.Remove(id))
        {
            nuevas.Add(id);
        }
        await OnCambio.InvokeAsync(nuevas);
    }

    private async Task Limpiar()
    {
        await OnCambio.InvokeAsync(new List<int>());
    }
}
