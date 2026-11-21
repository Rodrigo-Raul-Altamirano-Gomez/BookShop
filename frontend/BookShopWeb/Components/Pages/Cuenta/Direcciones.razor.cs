using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Cuenta;

// RF02.6–7 y RF02.13 (UX-UI §9.10)
public partial class Direcciones
{
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private int idUsuario;
    private List<Direccion>? direcciones;
    private Direccion? direccionAEliminar;
    private int marcando;
    private DireccionModal modal = default!;
    private ConfirmModal confirmacion = default!;

    protected override async Task OnInitializedAsync()
    {
        idUsuario = SesionUsuario.Id((await AuthState).User);
        await Recargar();
    }

    private async Task Recargar()
    {
        direcciones = await new HttpClientUtils<List<Direccion>>().Get($"direcciones/usuario/{idUsuario}");
    }

    private async Task MarcarPredeterminada(Direccion direccion)
    {
        marcando = direccion.Id;
        await new HttpClientUtils<object>().Put($"direcciones/{direccion.Id}/predeterminada", direccion);
        await Recargar();
        marcando = 0;
        Aviso.Mostrar("Dirección predeterminada actualizada");
    }

    private async Task PedirEliminar(Direccion direccion)
    {
        direccionAEliminar = direccion;
        await confirmacion.Mostrar($"¿Eliminar la dirección «{direccion.Alias}»?");
    }

    // RF02.13: eliminar la desactiva; desaparece de la lista de inmediato
    private async Task Eliminar()
    {
        await new HttpClientUtils<object>().Put($"direcciones/{direccionAEliminar!.Id}/desactivar", new { });
        direcciones!.Remove(direccionAEliminar);
        Aviso.Mostrar("Dirección eliminada");
    }
}
