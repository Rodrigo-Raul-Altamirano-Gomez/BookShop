using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Layout;

public partial class AdminLayout
{
    [Inject] private NavigationManager Navegacion { get; set; } = default!;

    private void CerrarSesion()
    {
        Navegacion.NavigateTo("auth/logout", forceLoad: true);
    }
}
