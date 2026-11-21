using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Routing;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Shared;

public partial class CompareBar : IDisposable
{
    [Inject] private ComparacionService Comparacion { get; set; } = default!;
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private IJSRuntime JS { get; set; } = default!;

    private LibroComparado? nuevo;

    protected override void OnInitialized()
    {
        Comparacion.Cambio += Refrescar;
        Comparacion.ReemplazoPedido += PedirReemplazo;
        Navegacion.LocationChanged += AlNavegar;
    }

    private async void PedirReemplazo(LibroComparado libro)
    {
        nuevo = libro;
        await InvokeAsync(StateHasChanged);
        await JS.InvokeVoidAsync("abrirModal", "bk-reemplazo");
    }

    private async Task Reemplazar(int idAnterior)
    {
        await JS.InvokeVoidAsync("cerrarModal", "bk-reemplazo");
        await Comparacion.Reemplazar(idAnterior, nuevo!);
    }

    private void Refrescar() => InvokeAsync(StateHasChanged);

    private void AlNavegar(object? sender, LocationChangedEventArgs e) => InvokeAsync(StateHasChanged);

    public void Dispose()
    {
        Comparacion.Cambio -= Refrescar;
        Comparacion.ReemplazoPedido -= PedirReemplazo;
        Navegacion.LocationChanged -= AlNavegar;
    }
}
