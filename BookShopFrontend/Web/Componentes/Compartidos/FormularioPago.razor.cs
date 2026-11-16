using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class FormularioPago
{
    [Parameter]
    public MetodoPago? Valor { get; set; }

    [Parameter]
    public EventCallback<MetodoPago> AlGuardar { get; set; }

    [Parameter]
    public EventCallback AlCerrar { get; set; }
    private MetodoPago Borrador { get; set; } = new();
    private DateOnly? FechaVencimiento { get; set; }
    private string MensajeError { get; set; } = "";
    private bool Guardando { get; set; }
    protected override void OnInitialized()
    {
        if (Valor is not null)
        {
            Borrador = new MetodoPago
            {
                Id = Valor.Id,
                Tipo = Valor.Tipo,
                Alias = Valor.Alias,
                Titular = Valor.Titular,
                Proveedor = Valor.Proveedor,
                TipoTarjeta = Valor.TipoTarjeta,
                NumeroEnmascarado = Valor.NumeroEnmascarado,
                Vencimiento = Valor.Vencimiento,
                Predeterminada = Valor.Predeterminada

            };
            FechaVencimiento = Valor.Vencimiento;
        }
    }

    private void MostrarTarjeta()
    {
        Borrador.Tipo = "Tarjeta";
        Borrador.Numero = "";
        MensajeError = "";
    }

    private void MostrarBilletera()
    {
        Borrador.Tipo = "Billetera";
        Borrador.Numero = "";
        MensajeError = "";
    }

    private async Task Guardar()
    {
        if (Guardando)
            return;
        if (FechaVencimiento.HasValue && Borrador.Tipo == "Tarjeta")
            Borrador.Vencimiento = FechaVencimiento.Value;
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
