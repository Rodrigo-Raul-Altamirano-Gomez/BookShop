using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class FormularioDireccion
{
    [Parameter]
    public Direccion? Valor
    {
        get; set;
    }

    [Parameter]
    public EventCallback<Direccion> AlGuardar
    {
        get; set;
    }

    [Parameter]
    public EventCallback AlCerrar
    {
        get; set;
    }
    private Direccion Borrador { get; set; } = new();
    private string MensajeError { get; set; } = "";
    private bool Guardando
    {
        get; set;
    }
    private bool Completo => !string.IsNullOrWhiteSpace(Borrador.Alias) && !string.IsNullOrWhiteSpace(
        Borrador.Calle) && !string.IsNullOrWhiteSpace(Borrador.Distrito) && !string.IsNullOrWhiteSpace(
            Borrador.Ciudad);

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
        await AlGuardar.InvokeAsync(Borrador);
        Guardando = false;
    }
}

