using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Compartidos;

public partial class Modal
{
    [Parameter]
    public string Titulo { get; set; } = "";

    [Parameter]
    public bool Amplio { get; set; }

    [Parameter]
    public EventCallback AlCerrar { get; set; }

    [Parameter]
    public RenderFragment? ChildContent { get; set; }

    [Parameter] public bool Ocupado { get; set; }
    private Task Cerrar() => Ocupado ? Task.CompletedTask : AlCerrar.InvokeAsync();

    private readonly string IdTitulo = "dialog-" + Guid.NewGuid().ToString("N");
}
