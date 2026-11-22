using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Cuenta;

// RF03.3, RF03.5–7 y RF17 (UX-UI §9.3)
public partial class PedidoPagina
{
    [Parameter] public int Id { get; set; }
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    private PedidoDetalle? detalle;
    private bool noEncontrado;
    private MotivoDevolucion motivo;
    private string comentario = "";
    private bool enviando;
    private ConfirmModal confirmacion = default!;

    protected override async Task OnParametersSetAsync()
    {
        try
        {
            await Recargar();
        }
        catch (ApiException ex) when (ex.Estado == 404)
        {
            noEncontrado = true;
        }
    }

    private async Task Recargar()
    {
        detalle = await new HttpClientUtils<PedidoDetalle>().Get($"pedidos/{Id}");
    }

    private async Task PedirCancelar()
    {
        int restantes = detalle!.CancelacionesRestantes;
        string quedan = restantes == 1 ? "Te queda 1 cancelación este mes." : $"Te quedan {restantes} cancelaciones este mes.";
        await confirmacion.Mostrar($"¿Cancelar el pedido N.º {Id}?", "Se repondrá el stock y no podrás deshacerlo. " + quedan);
    }

    // RF03.5: el seguimiento pudo avanzar mientras la página estaba abierta; en ese caso se muestra el motivo
    private async Task Cancelar()
    {
        try
        {
            await new HttpClientUtils<object>().Put($"pedidos/{Id}/cancelar", new { });
            Aviso.Mostrar("Pedido cancelado. El stock fue repuesto.");
        }
        catch (ApiException ex) when (ex.Estado == 422)
        {
            Aviso.Mostrar(ex.Message);
        }
        await Recargar();
    }

    private async Task AbrirDevolucion()
    {
        motivo = MotivoDevolucion.PRODUCTO_DANADO;
        comentario = "";
        await JS.InvokeVoidAsync("abrirModal", "m-dev");
    }

    private async Task EnviarDevolucion()
    {
        enviando = true;
        SolicitudDevolucion solicitud = new SolicitudDevolucion
        {
            Pedido = new Pedido { Id = Id },
            Motivo = motivo,
            Comentario = comentario.Trim()
        };
        try
        {
            await new HttpClientUtils<SolicitudDevolucion>().Post("devoluciones", solicitud);
            Aviso.Mostrar("Solicitud enviada. Puedes verla en Mis solicitudes.", "Ver", "cuenta/solicitudes");
        }
        catch (ApiException ex) when (ex.Estado == 422)
        {
            Aviso.Mostrar(ex.Message);
        }
        await Recargar();
        await JS.InvokeVoidAsync("cerrarModal", "m-dev");
        enviando = false;
    }
}
