using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.AspNetCore.Components.Routing;

namespace BookShopWeb.Components.Layout;

public partial class Encabezado : IDisposable
{
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private UsuarioActual UsuarioActual { get; set; } = default!;
    [Inject] private ComparacionService Comparacion { get; set; } = default!;
    [Inject] private CarritoService Carrito { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private string busqueda = "";

    private bool EnExplorar => Navegacion.Uri.Contains("/explorar") || Navegacion.Uri.Contains("/autores/")
                               || Navegacion.Uri.Contains("/categorias/") || Navegacion.Uri.Contains("/editoriales/");

    protected override async Task OnInitializedAsync()
    {
        UsuarioActual.Cambio += Refrescar;
        Carrito.Cambio += Refrescar;
        Navegacion.LocationChanged += AlNavegar;
        var usuario = (await AuthState).User;
        if (usuario.Identity?.IsAuthenticated == true)
        {
            await UsuarioActual.Cargar(SesionUsuario.Id(usuario));
        }
        await Comparacion.Cargar();
        await Carrito.Cargar();
    }

    private void Buscar()
    {
        Navegacion.NavigateTo("catalogo?q=" + Uri.EscapeDataString(busqueda.Trim()));
        busqueda = "";
    }

    private string UrlLogin()
    {
        return "login?aviso=1&volver=" + Uri.EscapeDataString("/" + Navegacion.ToBaseRelativePath(Navegacion.Uri));
    }

    // Cerrar sesión recarga la página: el servidor borra la cookie
    private void CerrarSesion()
    {
        Navegacion.NavigateTo("auth/logout", forceLoad: true);
    }

    private static string PrimerNombre(string? nombre)
    {
        return string.IsNullOrEmpty(nombre) ? "" : nombre.Split(' ')[0];
    }

    private void Refrescar() => InvokeAsync(StateHasChanged);

    private void AlNavegar(object? sender, LocationChangedEventArgs e) => InvokeAsync(StateHasChanged);

    public void Dispose()
    {
        UsuarioActual.Cambio -= Refrescar;
        Carrito.Cambio -= Refrescar;
        Navegacion.LocationChanged -= AlNavegar;
    }
}
