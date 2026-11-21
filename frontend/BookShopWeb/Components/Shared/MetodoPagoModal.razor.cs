using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Shared;

public partial class MetodoPagoModal
{
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    [Parameter, EditorRequired] public int IdUsuario { get; set; }
    [Parameter] public string TextoGuardado { get; set; } = "Método de pago guardado";
    [Parameter] public EventCallback<MetodoPago> OnGuardado { get; set; }

    private MetodoPago metodo = new();
    private string? errorNumero;
    private bool guardando;

    public async Task Abrir(MetodoPago? existente = null)
    {
        metodo = new MetodoPago { Usuario = new Usuario { Id = IdUsuario }, Tipo = TipoMetodoPago.TARJETA };
        if (existente != null)
        {
            metodo.Id = existente.Id;
            metodo.Tipo = existente.Tipo;
            metodo.Titular = existente.Titular;
            metodo.Numero = existente.Numero;
            metodo.FechaVencimiento = existente.FechaVencimiento;
            metodo.Proveedor = existente.Proveedor;
            metodo.Predeterminado = existente.Predeterminado;
        }
        errorNumero = null;
        StateHasChanged();
        await JS.InvokeVoidAsync("abrirModal", "m-metodo");
    }

    private void CambiarTipo(TipoMetodoPago tipo)
    {
        metodo.Tipo = tipo;
        metodo.Numero = "";
        errorNumero = null;
        if (tipo == TipoMetodoPago.BILLETERA && metodo.Proveedor == null)
        {
            metodo.Proveedor = "Yape";
        }
    }

    private async Task Guardar()
    {
        // Cada tipo guarda solo sus datos (ERS RF02.8)
        if (metodo.Tipo == TipoMetodoPago.TARJETA)
        {
            metodo.Proveedor = null;
        }
        else
        {
            metodo.FechaVencimiento = null;
        }
        guardando = true;
        errorNumero = null;
        try
        {
            if (metodo.Id == 0)
            {
                metodo = (await new HttpClientUtils<MetodoPago>().Post("metodos-pago", metodo))!;
            }
            else
            {
                await new HttpClientUtils<object>().Put($"metodos-pago/{metodo.Id}", metodo);
            }
            await JS.InvokeVoidAsync("cerrarModal", "m-metodo");
            Aviso.Mostrar(TextoGuardado);
            await OnGuardado.InvokeAsync(metodo);
        }
        catch (ApiException ex) when (ex.Estado == 409)
        {
            // RN04: número repetido
            errorNumero = ex.Message;
            await JS.InvokeVoidAsync("enfocar", metodo.Tipo == TipoMetodoPago.TARJETA ? "#m-num" : "#b-cel");
        }
        guardando = false;
    }
}
