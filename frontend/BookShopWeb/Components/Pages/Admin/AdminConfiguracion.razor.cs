using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Admin;

// RF04.10 y UX-UI §10.7: consulta y edición de los parámetros del ERS §3.1
public partial class AdminConfiguracion
{
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    private Configuracion? configuracion;
    private Configuracion edicion = new();
    private bool editando;
    private bool guardando;

    protected override async Task OnInitializedAsync()
    {
        configuracion = await new HttpClientUtils<Configuracion>().Get("configuracion");
    }

    private async Task Editar()
    {
        edicion = new Configuracion
        {
            CostoEnvio = configuracion!.CostoEnvio,
            DiasDevolucion = configuracion.DiasDevolucion,
            MaxSolicitudesDevolucion = configuracion.MaxSolicitudesDevolucion,
            MaxCancelacionesMes = configuracion.MaxCancelacionesMes,
            CuponMontoMinimo = configuracion.CuponMontoMinimo,
            CuponCadaPedidos = configuracion.CuponCadaPedidos,
            CuponPorcentaje = configuracion.CuponPorcentaje,
            CuponDiasVigencia = configuracion.CuponDiasVigencia
        };
        editando = true;
        await JS.InvokeVoidAsync("enfocar", "#env");
    }

    private async Task Guardar()
    {
        guardando = true;
        await new HttpClientUtils<object>().Put("configuracion", edicion);
        configuracion = edicion;
        guardando = false;
        editando = false;
        Aviso.Mostrar("Configuración guardada");
    }
}
