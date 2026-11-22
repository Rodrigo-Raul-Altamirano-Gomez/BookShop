using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Security;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Cuenta;

public partial class Resenas
{
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private List<Resena>? resenas;

    protected override async Task OnInitializedAsync()
    {
        int idUsuario = SesionUsuario.Id((await AuthState).User);
        resenas = await new HttpClientUtils<List<Resena>>().Get($"resenas/usuario/{idUsuario}");
    }
}
