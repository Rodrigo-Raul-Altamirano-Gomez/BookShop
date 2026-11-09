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

    protected override async Task OnInitializedAsync()
    {
        correoCargado = Sesion.Usuario?.Correo;
        await CargarDatos();
    }

    protected virtual Task CargarDatos() => Task.CompletedTask;

    private void Actualizar() => _ = InvokeAsync(async () =>
    {
        if (correoCargado != Sesion.Usuario?.Correo)
        {
            correoCargado = Sesion.Usuario?.Correo;
            await CargarDatos();
        }
        StateHasChanged();
    });
    public void Dispose() => Sesion.Cambiado -= Actualizar;
}

