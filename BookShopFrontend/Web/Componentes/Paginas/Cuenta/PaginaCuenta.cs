using Microsoft.AspNetCore.Components;
using Web.Servicios;

namespace Web.Componentes.Paginas.Cuenta;
// Las pantallas personales actualizan su contenido al cambiar la sesión.
public class PaginaCuenta : ComponentBase, IDisposable
{
    [Inject]
    protected SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    protected NavigationManager Navegacion { get; set; } = default!;

    protected override void OnInitialized() => Sesion.Cambiado += Actualizar;
    private string? correoCargado;
    protected bool CargandoDatos { get; private set; } = true;
    protected string? ErrorCarga { get; set; }

    protected override async Task OnInitializedAsync()
    {
        correoCargado = Sesion.Usuario?.Correo;
        await ActualizarDatos();
    }

    private async Task ActualizarDatos()
    {
        CargandoDatos = true;
        ErrorCarga = null;
        StateHasChanged();
        try
        {
            await CargarDatos();
        }
        catch (InvalidOperationException e)
        {
            ErrorCarga = e.Message;
        }
        finally
        {
            CargandoDatos = false;
        }
    }

    protected virtual Task CargarDatos() => Task.CompletedTask;

    private async void Actualizar()
    {
        await InvokeAsync(async () =>
        {
            if (correoCargado != Sesion.Usuario?.Correo)
            {
                correoCargado = Sesion.Usuario?.Correo;
                await ActualizarDatos();
            }
            StateHasChanged();
        });
    }
    public void Dispose() => Sesion.Cambiado -= Actualizar;
}
