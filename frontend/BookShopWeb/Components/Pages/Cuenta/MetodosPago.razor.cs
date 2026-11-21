using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Cuenta;

// RF02.8–11 y RF02.13 (UX-UI §9.11)
public partial class MetodosPago
{
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private int idUsuario;
    private List<MetodoPago>? metodos;
    private MetodoPago? metodoAEliminar;
    private int marcando;
    private MetodoPagoModal modal = default!;
    private ConfirmModal confirmacion = default!;

    protected override async Task OnInitializedAsync()
    {
        idUsuario = SesionUsuario.Id((await AuthState).User);
        await Recargar();
    }

    private async Task Recargar()
    {
        metodos = await new HttpClientUtils<List<MetodoPago>>().Get($"metodos-pago/usuario/{idUsuario}");
    }

    private async Task MarcarPredeterminado(MetodoPago metodo)
    {
        marcando = metodo.Id;
        await new HttpClientUtils<object>().Put($"metodos-pago/{metodo.Id}/predeterminado", metodo);
        await Recargar();
        marcando = 0;
        Aviso.Mostrar("Método predeterminado actualizado");
    }

    private async Task PedirEliminar(MetodoPago metodo)
    {
        metodoAEliminar = metodo;
        string nombre = metodo.Tipo == TipoMetodoPago.TARJETA ? "la tarjeta" : metodo.Proveedor!;
        await confirmacion.Mostrar($"¿Eliminar {nombre} •••• {metodo.Numero[^4..]}?");
    }

    // RF02.13: eliminar lo desactiva
    private async Task Eliminar()
    {
        await new HttpClientUtils<object>().Put($"metodos-pago/{metodoAEliminar!.Id}/desactivar", new { });
        metodos!.Remove(metodoAEliminar);
        Aviso.Mostrar("Método de pago eliminado");
    }
}
