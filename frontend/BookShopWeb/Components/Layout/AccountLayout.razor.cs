using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Layout;

public partial class AccountLayout : IDisposable
{
    [Inject] private UsuarioActual UsuarioActual { get; set; } = default!;
    [Inject] private NavigationManager Navegacion { get; set; } = default!;

    protected override void OnInitialized()
    {
        UsuarioActual.Cambio += Refrescar;
    }

    private void CerrarSesion()
    {
        Navegacion.NavigateTo("auth/logout", forceLoad: true);
    }

    private void Refrescar() => InvokeAsync(StateHasChanged);

    public void Dispose()
    {
        UsuarioActual.Cambio -= Refrescar;
    }
}
