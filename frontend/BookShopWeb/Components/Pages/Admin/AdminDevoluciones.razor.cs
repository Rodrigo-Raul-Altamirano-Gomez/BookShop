using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Pages.Admin;

// RF04.7 (UX-UI §10.5): aceptar o rechazar las solicitudes pendientes; tabla de 20 filas con la página en la URL
public partial class AdminDevoluciones
{
    private const int TamanoPagina = 20;

    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    [SupplyParameterFromQuery] public int? Pagina { get; set; }

    private ResultadoPagina<SolicitudDevolucion>? resultado;
    private SolicitudDevolucion? elegida;
    private bool aceptar;
    private ConfirmModal confirmacion = default!;

    private int TotalPaginas => Math.Max(1, (resultado!.Total + TamanoPagina - 1) / TamanoPagina);

    protected override async Task OnParametersSetAsync()
    {
        resultado = await new HttpClientUtils<ResultadoPagina<SolicitudDevolucion>>().Get($"devoluciones/pendientes?pagina={Pagina ?? 1}&tamano={TamanoPagina}");
    }

    private static string Ruta(int pagina) => pagina > 1 ? $"admin/devoluciones?pagina={pagina}" : "admin/devoluciones";

    // "1–20 de 36"
    private string Rango()
    {
        int desde = ((Pagina ?? 1) - 1) * TamanoPagina + 1;
        int hasta = desde + resultado!.Elementos.Count - 1;
        return $"{desde}–{hasta} de {resultado.Total}";
    }

    private async Task PedirAceptar(SolicitudDevolucion solicitud)
    {
        elegida = solicitud;
        aceptar = true;
        await confirmacion.Mostrar($"¿Aceptar la devolución del pedido N.º {solicitud.Pedido.Id}?",
            "El pedido pasará a «Devuelto» y se repondrá el stock de sus libros.", "Sí, aceptar", false);
    }

    private async Task PedirRechazar(SolicitudDevolucion solicitud)
    {
        elegida = solicitud;
        aceptar = false;
        await confirmacion.Mostrar($"¿Rechazar la devolución del pedido N.º {solicitud.Pedido.Id}?",
            "El pedido seguirá como «Entregado». El cliente verá la solicitud como rechazada.", "Sí, rechazar", true);
    }

    // RF03.8: aceptar deja el pedido DEVUELTO y repone el stock. La fila sale de la lista.
    private async Task Resolver()
    {
        if (aceptar)
        {
            await new HttpClientUtils<object>().Put($"devoluciones/{elegida!.Id}/aceptar", elegida);
            Aviso.Mostrar("Devolución aceptada");
        }
        else
        {
            await new HttpClientUtils<object>().Put($"devoluciones/{elegida!.Id}/rechazar", new { });
            Aviso.Mostrar("Devolución rechazada");
        }
        resultado!.Elementos.Remove(elegida);
        resultado = resultado with { Total = resultado.Total - 1 };
    }
}
