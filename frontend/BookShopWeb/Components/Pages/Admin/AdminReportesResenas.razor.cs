using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Pages.Admin;

// RF04.8 (UX-UI §10.6): aceptar oculta la reseña; rechazar la deja visible. Páginas de 10 tarjetas.
public partial class AdminReportesResenas
{
    private const int TamanoPagina = 10;

    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    [SupplyParameterFromQuery] public int? Pagina { get; set; }

    private ResultadoPagina<ReporteResena>? resultado;
    private ReporteResena? elegido;
    private bool ocultar;
    private ConfirmModal confirmacion = default!;

    private int TotalPaginas => Math.Max(1, (resultado!.Total + TamanoPagina - 1) / TamanoPagina);

    protected override async Task OnParametersSetAsync()
    {
        resultado = await new HttpClientUtils<ResultadoPagina<ReporteResena>>().Get($"reportes-resenas/pendientes?pagina={Pagina ?? 1}&tamano={TamanoPagina}");
    }

    private static string Ruta(int pagina) => pagina > 1 ? $"admin/reportes-resenas?pagina={pagina}" : "admin/reportes-resenas";

    // "1–10 de 12"
    private string Rango()
    {
        int desde = ((Pagina ?? 1) - 1) * TamanoPagina + 1;
        int hasta = desde + resultado!.Elementos.Count - 1;
        return $"{desde}–{hasta} de {resultado.Total}";
    }

    private async Task PedirOcultar(ReporteResena reporte)
    {
        elegido = reporte;
        ocultar = true;
        await confirmacion.Mostrar("¿Ocultar esta reseña?", "Dejará de mostrarse y de contar en el promedio del libro. El reporte quedará aceptado.", "Sí, ocultar", false);
    }

    private async Task PedirMantener(ReporteResena reporte)
    {
        elegido = reporte;
        ocultar = false;
        await confirmacion.Mostrar("¿Mantener visible esta reseña?", "El reporte quedará rechazado.", "Sí, mantener", false);
    }

    // La tarjeta sale de la lista y se avisa (UX-UI §4.4)
    private async Task Resolver()
    {
        if (ocultar)
        {
            await new HttpClientUtils<object>().Put($"reportes-resenas/{elegido!.Id}/aceptar", elegido);
            Aviso.Mostrar("Reseña ocultada");
        }
        else
        {
            await new HttpClientUtils<object>().Put($"reportes-resenas/{elegido!.Id}/rechazar", new { });
            Aviso.Mostrar("Reporte rechazado");
        }
        resultado!.Elementos.Remove(elegido);
        resultado = resultado with { Total = resultado.Total - 1 };
    }
}
