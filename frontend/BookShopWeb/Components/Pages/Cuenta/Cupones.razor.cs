using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Security;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Cuenta;

public partial class Cupones
{
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private List<Cupon>? cupones;

    protected override async Task OnInitializedAsync()
    {
        int idUsuario = SesionUsuario.Id((await AuthState).User);
        cupones = await new HttpClientUtils<List<Cupon>>().Get($"cupones/usuario/{idUsuario}");
    }
}
