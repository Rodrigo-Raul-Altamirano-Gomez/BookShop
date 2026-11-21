using BookShopWeb.Code;
using BookShopWeb.Models;
using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Cuenta;

// RF01.5–6 y UX-UI §8.8
public partial class Login
{
    [Inject] private IJSRuntime JS { get; set; } = default!;

    [SupplyParameterFromQuery] public int? Aviso { get; set; }
    [SupplyParameterFromQuery] public string? Volver { get; set; }
    [SupplyParameterFromQuery] public int? Error { get; set; }

    private string correo = "";
    private string contrasena = "";
    private bool verClave;
    private bool procesando;
    private bool reactivando;
    private bool mostrarError;

    protected override void OnInitialized()
    {
        mostrarError = Error == 1;
    }

    // Primero se valida con el backend; si todo está bien, el formulario va a /auth/login, que firma la sesión
    private async Task Ingresar()
    {
        procesando = true;
        mostrarError = false;
        try
        {
            await new HttpClientUtils<Usuario>().Post("usuarios/login", new Credenciales(correo, contrasena));
            await JS.InvokeVoidAsync("iniciarSesion", correo, contrasena, Volver);
        }
        catch (ApiException ex) when (ex.Estado == 401)
        {
            mostrarError = true;
            procesando = false;
        }
        catch (ApiException ex) when (ex.Estado == 403)
        {
            // RF01.6: la cuenta desactivada con la contraseña correcta puede reactivarse
            procesando = false;
            await JS.InvokeVoidAsync("abrirModal", "m-reactivar");
        }
    }

    private async Task Reactivar()
    {
        reactivando = true;
        await new HttpClientUtils<Usuario>().Put("usuarios/reactivar", new Credenciales(correo, contrasena));
        await JS.InvokeVoidAsync("iniciarSesion", correo, contrasena, Volver);
    }
}
