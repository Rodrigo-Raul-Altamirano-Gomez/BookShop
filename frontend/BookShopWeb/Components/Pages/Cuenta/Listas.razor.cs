using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Cuenta;

// Deseados y Favoritos (?lista=deseados|favoritos); por defecto, Deseados
public partial class Listas
{
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    [SupplyParameterFromQuery] public string? Lista { get; set; }

    private int idUsuario;
    private ResultsView vista = default!;

    private bool EsFavoritos => Lista == "favoritos";

    protected override async Task OnInitializedAsync()
    {
        idUsuario = SesionUsuario.Id((await AuthState).User);
    }

    // El libro sale de la lista de inmediato y se avisa (UX-UI §6.3)
    private async Task Quitar(Libro libro)
    {
        TipoLista lista = EsFavoritos ? TipoLista.FAVORITOS : TipoLista.DESEADOS;
        await new HttpClientUtils<object>().Delete($"listas/{lista}/usuario/{idUsuario}/libro/{libro.Id}");
        Aviso.Mostrar("Quitado de la lista");
        await vista.Recargar();
    }
}
