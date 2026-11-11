using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class FormularioDireccion
{
    [Parameter]
    public Direccion? Valor { get; set; }

    [Parameter]
    public EventCallback<Direccion> AlGuardar { get; set; }

    [Parameter]
    public EventCallback AlCerrar { get; set; }
    private Direccion Borrador { get; set; } = new();
    private string MensajeError { get; set; } = "";
    private bool Guardando { get; set; }
    private bool Completo
    {
        get
        {
            if (string.IsNullOrWhiteSpace(Borrador.Alias))
                return false;
            if (string.IsNullOrWhiteSpace(Borrador.Calle))
                return false;
            if (string.IsNullOrWhiteSpace(Borrador.Distrito))
                return false;
            return !string.IsNullOrWhiteSpace(Borrador.Ciudad);
        }
    }

    protected override void OnInitialized()
    {
        if (Valor is not null)
            Borrador = new Direccion
            {
                Id = Valor.Id,
                Alias = Valor.Alias,
                Calle = Valor.Calle,
                Distrito = Valor.Distrito,
                Ciudad = Valor.Ciudad,
                ReferenciaAdicional = Valor.ReferenciaAdicional,
                Predeterminada = Valor.Predeterminada

            };
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
        finally
        {
            Guardando = false;
        }
    }
}
