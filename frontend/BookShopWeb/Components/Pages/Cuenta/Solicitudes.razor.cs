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

    private List<SolicitudDevolucion>? solicitudes;
    private SolicitudDevolucion? elegida;
    private MotivoDevolucion motivo;
    private string comentario = "";
    private bool guardando;
    private ConfirmModal confirmacion = default!;

    private bool EnReportes => Tipo == "reportes";

    protected override async Task OnParametersSetAsync()
    {
        if (!EnReportes && solicitudes == null)
        {
            int idUsuario = SesionUsuario.Id((await AuthState).User);
            solicitudes = await new HttpClientUtils<List<SolicitudDevolucion>>().Get($"devoluciones/usuario/{idUsuario}");
        }
    }

    private async Task AbrirEdicion(SolicitudDevolucion solicitud)
    {
        elegida = solicitud;
        motivo = solicitud.Motivo;
        comentario = solicitud.Comentario;
        await JS.InvokeVoidAsync("abrirModal", "m-edit");
        await JS.InvokeVoidAsync("enfocar", "#e-mot");
    }

    private async Task Guardar()
    {
        guardando = true;
        SolicitudDevolucion cambios = new SolicitudDevolucion { Motivo = motivo, Comentario = comentario.Trim() };
        await new HttpClientUtils<object>().Put($"devoluciones/{elegida!.Id}", cambios);
        elegida.Motivo = cambios.Motivo;
        elegida.Comentario = cambios.Comentario;
        await JS.InvokeVoidAsync("cerrarModal", "m-edit");
        guardando = false;
        Aviso.Mostrar("Cambios guardados");
    }

    private async Task PedirRetiro(SolicitudDevolucion solicitud)
    {
        elegida = solicitud;
        await confirmacion.Mostrar();
    }

    private async Task Retirar()
    {
        await new HttpClientUtils<object>().Put($"devoluciones/{elegida!.Id}/retirar", new { });
        elegida.Estado = EstadoSolicitud.RETIRADA;
        Aviso.Mostrar("Solicitud retirada");
    }
}
