using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Shared;

public partial class DescuentoModal
{
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    [Parameter] public EventCallback<Descuento> OnGuardado { get; set; }

    private Libro? libro;
    private int? porcentaje;
    private DateOnly? desde;
    private DateOnly? hasta;
    private string? error;
    private bool guardando;

    public async Task Abrir(Libro libroElegido)
    {
        libro = libroElegido;
        porcentaje = null;
        desde = null;
        hasta = null;
        error = null;
        StateHasChanged();
        await JS.InvokeVoidAsync("abrirModal", "m-descuento");
        await JS.InvokeVoidAsync("enfocar", "#dp");
    }

    private async Task Guardar()
    {
        guardando = true;
        error = null;
        Descuento descuento = new Descuento
        {
            Libro = new Libro { Id = libro!.Id },
            Porcentaje = porcentaje!.Value,
            FechaInicio = desde!.Value,
            FechaFin = hasta!.Value
        };
        try
        {
            Descuento? guardado = await new HttpClientUtils<Descuento>().Post("descuentos", descuento);
            await JS.InvokeVoidAsync("cerrarModal", "m-descuento");
            Aviso.Mostrar("Descuento agregado");
            await OnGuardado.InvokeAsync(guardado);
        }
        catch (ApiException ex) when (ex.Estado == 409)
        {
            // RN05: se superpone con otro descuento del libro
            error = ex.Message;
            await JS.InvokeVoidAsync("enfocar", "#dd");
        }
        guardando = false;
    }
}
