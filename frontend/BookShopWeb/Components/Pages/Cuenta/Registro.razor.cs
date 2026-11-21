using BookShopWeb.Code;
using BookShopWeb.Models;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Cuenta;

// RF01.1–4 y UX-UI §8.9
public partial class Registro
{
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private IWebHostEnvironment Entorno { get; set; } = default!;

    private Usuario usuario = new();
    private DateOnly? fechaNacimiento;
    private string contrasena = "";
    private string confirmacion = "";
    private string? errorCorreo;
    private bool errorConfirmacion;
    private bool procesando;

    private async Task ElegirFoto(InputFileChangeEventArgs e)
    {
        usuario.Foto = await Imagenes.Guardar(e.File, Entorno.WebRootPath);
    }

    private async Task Registrar()
    {
        errorCorreo = null;
        errorConfirmacion = contrasena != confirmacion;
        if (errorConfirmacion)
        {
            await JS.InvokeVoidAsync("enfocar", "#c2");
            return;
        }
        procesando = true;
        try
        {
            usuario.FechaNacimiento = fechaNacimiento!.Value;
            usuario.Contrasena = contrasena;
            await new HttpClientUtils<Usuario>().Post("usuarios", usuario);
            // RF01.4: al registrarse queda con la sesión iniciada
            await JS.InvokeVoidAsync("iniciarSesion", usuario.Correo, contrasena, "/");
        }
        catch (ApiException ex) when (ex.Estado == 409)
        {
            // RN01: el correo ya está registrado
            errorCorreo = ex.Message;
            procesando = false;
            await JS.InvokeVoidAsync("enfocar", "#correo");
        }
    }
}
