using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Security;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Tienda;

// Página de un autor, una categoría o una editorial (UX-UI §8.6)
public partial class EntidadPagina
{
    [Parameter] public int Id { get; set; }
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private Entidad? entidad;
    private bool noEncontrado;
    private int idUsuario;          // 0: visitante
    private bool esAdmin;
    private bool siguiendo;
    private bool cambiando;

    protected override async Task OnParametersSetAsync()
    {
        if (entidad?.Id == Id) return;
        var usuario = (await AuthState).User;
        idUsuario = usuario.Identity?.IsAuthenticated == true ? SesionUsuario.Id(usuario) : 0;
        esAdmin = SesionUsuario.EsAdmin(usuario);
        entidad = null;
        noEncontrado = false;
        try
        {
            entidad = await new HttpClientUtils<Entidad>().Get($"entidades/{Id}");
        }
        catch (ApiException ex) when (ex.Estado == 404)
        {
            noEncontrado = true;
            return;
        }
        if (idUsuario > 0 && !esAdmin)
        {
            siguiendo = await new HttpClientUtils<bool>().Get($"seguimientos/usuario/{idUsuario}/entidad/{Id}");
        }
    }

    // El visitante va a iniciar sesión (UX-UI §3.3)
    private async Task AlternarSeguimiento()
    {
        if (idUsuario == 0)
        {
            string volver = "/" + Navegacion.ToBaseRelativePath(Navegacion.Uri);
            Navegacion.NavigateTo("login?aviso=1&volver=" + Uri.EscapeDataString(volver));
            return;
        }
        cambiando = true;
        string ruta = $"seguimientos/usuario/{idUsuario}/entidad/{Id}";
        if (siguiendo)
        {
            await new HttpClientUtils<object>().Delete(ruta);
        }
        else
        {
            await new HttpClientUtils<object>().Put(ruta, new { });
        }
        siguiendo = !siguiendo;
        cambiando = false;
    }
}
