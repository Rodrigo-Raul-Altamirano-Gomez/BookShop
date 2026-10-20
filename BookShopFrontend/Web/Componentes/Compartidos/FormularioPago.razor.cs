using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class FormularioPago
{
    [Parameter]
    public MetodoPago? Valor
    {
        get; set;
    }

    [Parameter]
    public EventCallback<MetodoPago> AlGuardar
    {
        get; set;
    }

    [Parameter]
    public EventCallback AlCerrar
    {
        get; set;
    }
    private MetodoPago Borrador { get; set; } = new();
    private string Numero { get; set; } = "";
    private DateOnly? FechaVencimiento
    {
        get; set;
    }
    private string MensajeError { get; set; } = "";
    private bool Guardando
    {
        get; set;
    }
    private bool Completo => !string.IsNullOrWhiteSpace(Borrador.Titular) && !string.IsNullOrWhiteSpace(
        Borrador.Proveedor) && (Valor is not null || !string.IsNullOrWhiteSpace(Numero)) && (Borrador
            .Tipo != "Tarjeta" || FechaVencimiento.HasValue);

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

        var digitos = Numero.Replace(" ", "").Replace("-", "");
        if (Numero.Length > 0 && (!digitos.All(char.IsDigit) || (Borrador.Tipo == "Tarjeta" ? digitos
            .Length != 16 : digitos.Length != 9)))
        {
            MensajeError = Borrador.Tipo == "Tarjeta" ? "Ingresa un número de tarjeta de 16 dígitos."
                : "Ingresa un celular de 9 dígitos.";
            return;
        }

        if (digitos.Length > 0)
            Borrador.NumeroEnmascarado = "•••• " + digitos[^Math.Min(4, digitos.Length)..];
        if (FechaVencimiento.HasValue && Borrador.Tipo == "Tarjeta")
            Borrador.Vencimiento = FechaVencimiento.Value;
        Guardando = true;
        await AlGuardar.InvokeAsync(Borrador);
        Guardando = false;
    }
}

