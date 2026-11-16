using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Compartidos;

public partial class FormularioPago
{
    [Parameter]
    public MetodoPagoDTO? Valor { get; set; }

    [Parameter]
    public EventCallback<DatosMetodoPagoDTO> AlGuardar { get; set; }

    [Parameter]
    public EventCallback AlCerrar { get; set; }
    private DatosMetodoPagoDTO Borrador { get; set; } = new() { TarjetaBancaria = true, TipoTarjeta = "DEBITO" };
    private string MensajeError { get; set; } = "";
    private bool Guardando { get; set; }
    protected override void OnInitialized()
    {
        if (Valor is not null)
        {
            Borrador = new DatosMetodoPagoDTO
            {
                TarjetaBancaria = Valor.TarjetaBancaria,
                Alias = Valor.Alias,
                Titular = Valor.Titular,
                Proveedor = Valor.Proveedor,
                TipoTarjeta = Valor.TipoTarjeta,
                FechaVencimiento = Valor.TarjetaBancaria ? Valor.FechaVencimiento : null,
                Predeterminado = Valor.Predeterminado

            };
        }
    }

    private void CambiarTipo(bool tarjeta)
    {
        if (Borrador.TarjetaBancaria == tarjeta)
            return;
        Borrador.TarjetaBancaria = tarjeta;
        Borrador.TipoTarjeta = tarjeta ? "DEBITO" : null;
        Borrador.FechaVencimiento = null;
        Borrador.Numero = "";
        MensajeError = "";
    }

    private async Task Guardar()
    {
        Guardando = true;
        MensajeError = "";
        try
        {
            await AlGuardar.InvokeAsync(Borrador);
        }
        catch (InvalidOperationException e)
        {
            MensajeError = e.Message;
        }
        Guardando = false;
    }
}
