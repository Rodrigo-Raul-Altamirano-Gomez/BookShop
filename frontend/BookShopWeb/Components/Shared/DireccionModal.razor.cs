using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Shared;

public partial class DireccionModal
{
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    [Parameter, EditorRequired] public int IdUsuario { get; set; }
    [Parameter] public string TextoGuardado { get; set; } = "Dirección guardada";
    [Parameter] public EventCallback<Direccion> OnGuardado { get; set; }

    private Direccion direccion = new();
    private bool guardando;

    // Sin dirección, abre el alta; con una dirección, su edición
    public async Task Abrir(Direccion? existente = null)
    {
        direccion = new Direccion { Usuario = new Usuario { Id = IdUsuario } };
        if (existente != null)
        {
            direccion.Id = existente.Id;
            direccion.Alias = existente.Alias;
            direccion.Detalle = existente.Detalle;
            direccion.Distrito = existente.Distrito;
            direccion.Ciudad = existente.Ciudad;
            direccion.Predeterminada = existente.Predeterminada;
        }
        StateHasChanged();
        await JS.InvokeVoidAsync("abrirModal", "m-direccion");
        await JS.InvokeVoidAsync("enfocar", "#d-alias");
    }

    private async Task Guardar()
    {
        guardando = true;
        if (direccion.Id == 0)
        {
            direccion = (await new HttpClientUtils<Direccion>().Post("direcciones", direccion))!;
        }
        else
        {
            await new HttpClientUtils<object>().Put($"direcciones/{direccion.Id}", direccion);
        }
        guardando = false;
        await JS.InvokeVoidAsync("cerrarModal", "m-direccion");
        Aviso.Mostrar(TextoGuardado);
        await OnGuardado.InvokeAsync(direccion);
    }
}
