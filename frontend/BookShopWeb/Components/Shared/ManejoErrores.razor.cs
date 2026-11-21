using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Routing;
using Microsoft.AspNetCore.Components.Web;

namespace BookShopWeb.Components.Shared;

public partial class ManejoErrores : IDisposable
{
    [Inject] private NavigationManager Navegacion { get; set; } = default!;

    [Parameter] public RenderFragment? ChildContent { get; set; }

    private ErrorBoundary errores = default!;

    protected override void OnInitialized()
    {
        Navegacion.LocationChanged += AlNavegar;
    }

    private void AlNavegar(object? sender, LocationChangedEventArgs e) => errores?.Recover();

    public void Dispose()
    {
        Navegacion.LocationChanged -= AlNavegar;
    }
}
