using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Cuenta;

// RF03.9–10 (UX-UI §9.4): pestañas Devoluciones y Reportes de reseñas (?tipo=reportes)
public partial class Solicitudes
{
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    [SupplyParameterFromQuery] public string? Tipo { get; set; }

    private int idUsuario;
    private List<SolicitudDevolucion>? solicitudes;
    private List<ReporteResena>? reportes;
    private SolicitudDevolucion? solicitudElegida;
    private ReporteResena? reporteElegido;
    private MotivoDevolucion motivo;
    private MotivoReporte motivoReporte;
    private string comentario = "";
    private bool guardando;
    private ConfirmModal confirmacion = default!;

    private bool EnReportes => Tipo == "reportes";

    // Cada pestaña se pide la primera vez que se abre
    protected override async Task OnParametersSetAsync()
    {
        idUsuario = SesionUsuario.Id((await AuthState).User);
        if (EnReportes && reportes == null)
        {
            reportes = await new HttpClientUtils<List<ReporteResena>>().Get($"reportes-resenas/usuario/{idUsuario}");
        }
        if (!EnReportes && solicitudes == null)
        {
            solicitudes = await new HttpClientUtils<List<SolicitudDevolucion>>().Get($"devoluciones/usuario/{idUsuario}");
        }
    }

    private async Task AbrirEdicion(SolicitudDevolucion solicitud)
    {
        solicitudElegida = solicitud;
        motivo = solicitud.Motivo;
        comentario = solicitud.Comentario;
        await JS.InvokeVoidAsync("abrirModal", "m-edit");
        await JS.InvokeVoidAsync("enfocar", "#e-mot");
    }

    private async Task AbrirEdicion(ReporteResena reporte)
    {
        reporteElegido = reporte;
        motivoReporte = reporte.Motivo;
        comentario = reporte.Comentario;
        await JS.InvokeVoidAsync("abrirModal", "m-edit");
        await JS.InvokeVoidAsync("enfocar", "#e-mot");
    }

    private async Task Guardar()
    {
        guardando = true;
        if (EnReportes)
        {
            ReporteResena cambios = new ReporteResena { Motivo = motivoReporte, Comentario = comentario.Trim() };
            await new HttpClientUtils<object>().Put($"reportes-resenas/{reporteElegido!.Id}", cambios);
            reporteElegido.Motivo = cambios.Motivo;
            reporteElegido.Comentario = cambios.Comentario;
        }
        else
        {
            SolicitudDevolucion cambios = new SolicitudDevolucion { Motivo = motivo, Comentario = comentario.Trim() };
            await new HttpClientUtils<object>().Put($"devoluciones/{solicitudElegida!.Id}", cambios);
            solicitudElegida.Motivo = cambios.Motivo;
            solicitudElegida.Comentario = cambios.Comentario;
        }
        await JS.InvokeVoidAsync("cerrarModal", "m-edit");
        guardando = false;
        Aviso.Mostrar("Cambios guardados");
    }

    private async Task PedirRetiro(SolicitudDevolucion solicitud)
    {
        solicitudElegida = solicitud;
        await confirmacion.Mostrar("¿Retirar esta solicitud?", "La solicitud pasará a «Retirada» y contará como un intento de devolución.");
    }

    private async Task PedirRetiro(ReporteResena reporte)
    {
        reporteElegido = reporte;
        await confirmacion.Mostrar("¿Retirar este reporte?", "Se retirará el reporte y la reseña no será revisada.");
    }

    private async Task Retirar()
    {
        if (EnReportes)
        {
            await new HttpClientUtils<object>().Put($"reportes-resenas/{reporteElegido!.Id}/retirar", new { });
            reporteElegido.Estado = EstadoSolicitud.RETIRADA;
            Aviso.Mostrar("Reporte retirado");
        }
        else
        {
            await new HttpClientUtils<object>().Put($"devoluciones/{solicitudElegida!.Id}/retirar", new { });
            solicitudElegida.Estado = EstadoSolicitud.RETIRADA;
            Aviso.Mostrar("Solicitud retirada");
        }
    }
}
