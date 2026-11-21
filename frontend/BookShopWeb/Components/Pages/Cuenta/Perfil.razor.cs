using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.AspNetCore.Components.Forms;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Cuenta;

// RF02.1–5 y UX-UI §9.9: consulta y edición en la misma pantalla
public partial class Perfil
{
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private IWebHostEnvironment Entorno { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [Inject] private UsuarioActual UsuarioActual { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;
    // ?editar=1 abre directamente la edición (UX-UI §7.1)
    [SupplyParameterFromQuery(Name = "editar")] public int? AbrirEnEdicion { get; set; }

    private int idUsuario;
    private Usuario? usuario;
    private Usuario edicion = new();
    private bool editando;
    private bool guardando;
    private ConfirmModal confirmacion = default!;

    // Cambio de contraseña
    private string actual = "";
    private string nueva = "";
    private string confirmacionNueva = "";
    private string? errorActual;
    private bool errorConfirmacion;
    private bool cambiandoClave;

    protected override async Task OnInitializedAsync()
    {
        idUsuario = SesionUsuario.Id((await AuthState).User);
        usuario = await new HttpClientUtils<Usuario>().Get($"usuarios/{idUsuario}");
        if (AbrirEnEdicion == 1)
        {
            await Editar();
        }
    }

    private async Task Editar()
    {
        edicion = new Usuario
        {
            Id = usuario!.Id,
            Correo = usuario.Correo,
            NombreCompleto = usuario.NombreCompleto,
            FechaNacimiento = usuario.FechaNacimiento,
            Foto = usuario.Foto
        };
        editando = true;
        // UX-UI §7.1: el foco va al primer campo
        await JS.InvokeVoidAsync("enfocar", "#n");
    }

    private async Task CambiarFoto(InputFileChangeEventArgs e)
    {
        edicion.Foto = await Imagenes.Guardar(e.File, Entorno.WebRootPath);
    }

    private async Task Guardar()
    {
        guardando = true;
        usuario = await new HttpClientUtils<Usuario>().Put($"usuarios/{idUsuario}", edicion);
        UsuarioActual.Actualizar(usuario!);
        guardando = false;
        editando = false;
        Aviso.Mostrar("Cambios guardados");
    }

    private async Task AbrirCambioContrasena()
    {
        actual = "";
        nueva = "";
        confirmacionNueva = "";
        errorActual = null;
        errorConfirmacion = false;
        await JS.InvokeVoidAsync("abrirModal", "m-clave");
    }

    private async Task CambiarContrasena()
    {
        errorActual = null;
        errorConfirmacion = nueva != confirmacionNueva;
        if (errorConfirmacion)
        {
            await JS.InvokeVoidAsync("enfocar", "#cc");
            return;
        }
        cambiandoClave = true;
        try
        {
            await new HttpClientUtils<object>().Put($"usuarios/{idUsuario}/contrasena", new CambioContrasena(actual, nueva));
            await JS.InvokeVoidAsync("cerrarModal", "m-clave");
            Aviso.Mostrar("Contraseña actualizada");
        }
        catch (ApiException ex) when (ex.Estado == 401)
        {
            // RF02.4: la contraseña actual no coincide
            errorActual = ex.Message;
            await JS.InvokeVoidAsync("enfocar", "#ca");
        }
        cambiandoClave = false;
    }

    // RF02.5: se desactiva y se cierra la sesión
    private async Task Desactivar()
    {
        await new HttpClientUtils<object>().Put($"usuarios/{idUsuario}/desactivar", new { });
        Navegacion.NavigateTo("auth/logout", forceLoad: true);
    }
}
