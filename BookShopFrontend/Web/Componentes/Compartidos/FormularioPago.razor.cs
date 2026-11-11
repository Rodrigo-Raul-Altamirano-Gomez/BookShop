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
    private string Numero { get; set; } = "";
    private DateOnly? FechaVencimiento { get; set; }
    private string MensajeError { get; set; } = "";
    private bool Guardando { get; set; }
    private bool Completo
    {
        get
        {
            if (string.IsNullOrWhiteSpace(Borrador.Titular))
                return false;
            if (string.IsNullOrWhiteSpace(Borrador.Proveedor))
                return false;
            if (Valor is null && string.IsNullOrWhiteSpace(Numero))
                return false;
            if (Borrador.Tipo == "Tarjeta" && !FechaVencimiento.HasValue)
                return false;
            return true;
        }
    }

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
        Numero = "";
        MensajeError = "";
    }

    private void MostrarBilletera()
    {
        Borrador.Tipo = "Billetera";
        Numero = "";
        MensajeError = "";
    }

    private async Task Guardar()
    {
        if (Guardando)
            return;
        if (!Completo)
        {
            MensajeError = "Completa los campos obligatorios.";
            return;
        }

        Borrador.Numero = string.IsNullOrWhiteSpace(Numero) ? null : Numero.Trim();
        if (FechaVencimiento.HasValue && Borrador.Tipo == "Tarjeta")
            Borrador.Vencimiento = FechaVencimiento.Value;
        Guardando = true;
        MensajeError = "";
        try
        {
            await AlGuardar.InvokeAsync(Borrador);
            Numero = "";
        }
        catch (InvalidOperationException e)
        {
            MensajeError = e.Message;
        }
        finally
        {
            Guardando = false;
        }
    }
}
