using Microsoft.AspNetCore.Components;
using Web.Demostracion;

namespace Web.Componentes.Paginas.Cuenta;
// Las pantallas personales escuchan el cambio de sesión de la demostración.
public class PaginaCuenta : ComponentBase, IDisposable
{
    [Inject]
    protected EstadoDemostracion Estado { get; set; } = default!;

    [Inject]
    protected NavigationManager Navegacion { get; set; } = default!;

    protected override void OnInitialized() => Estado.Cambiado += Actualizar;
    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    public void Dispose() => Estado.Cambiado -= Actualizar;
}

