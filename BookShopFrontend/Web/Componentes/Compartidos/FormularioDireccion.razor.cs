using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Compartidos;

public partial class FormularioDireccion
{
    [Parameter]
    public DireccionEnvioDTO? Valor { get; set; }

    [Parameter]
    public EventCallback<DatosDireccionEnvioDTO> AlGuardar { get; set; }

    [Parameter]
    public EventCallback AlCerrar { get; set; }
    private DatosDireccionEnvioDTO Borrador { get; set; } = new() { Ciudad = "Lima" };
    private string MensajeError { get; set; } = "";
    private bool Guardando { get; set; }
    protected override void OnInitialized()
    {
        if (Valor != null)
            Borrador = new DatosDireccionEnvioDTO
            {
                Alias = Valor.Alias,
                Calle = Valor.Calle,
                Distrito = Valor.Distrito,
                Ciudad = Valor.Ciudad,
                Referencia = Valor.Referencia,
                Predeterminada = Valor.Predeterminada

            };
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
