using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Shared;

public partial class ToastHost : IDisposable
{
    [Inject] private AvisoService Aviso { get; set; } = default!;

    protected override void OnInitialized()
    {
        Aviso.Cambio += Refrescar;
    }

    private void Refrescar() => InvokeAsync(StateHasChanged);

    public void Dispose()
    {
        Aviso.Cambio -= Refrescar;
    }
}
