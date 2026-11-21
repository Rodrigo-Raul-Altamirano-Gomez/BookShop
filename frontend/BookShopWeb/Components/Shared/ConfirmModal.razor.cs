using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Shared;

public partial class ConfirmModal
{
    [Inject] private IJSRuntime JS { get; set; } = default!;

    [Parameter] public string Titulo { get; set; } = "";
    [Parameter] public string? Mensaje { get; set; }
    [Parameter] public string TextoAccion { get; set; } = "Confirmar";
    [Parameter] public bool Peligro { get; set; }
    [Parameter] public EventCallback OnConfirmar { get; set; }

    private readonly string id = "confirmar-" + Guid.NewGuid().ToString("N")[..8];
    private string? titulo;
    private string? mensaje;
    private string? textoAccion;
    private bool? peligro;
    private bool procesando;

    // Los textos se pueden indicar al abrir, cuando dependen del registro elegido
    public async Task Mostrar(string? titulo = null, string? mensaje = null, string? textoAccion = null, bool? peligro = null)
    {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.textoAccion = textoAccion;
        this.peligro = peligro;
        StateHasChanged();
        await JS.InvokeVoidAsync("abrirModal", id);
    }

    public async Task Ocultar()
    {
        await JS.InvokeVoidAsync("cerrarModal", id);
    }

    private async Task Confirmar()
    {
        procesando = true;
        try
        {
            await OnConfirmar.InvokeAsync();
        }
        finally
        {
            procesando = false;
            await Ocultar();
        }
    }
}
