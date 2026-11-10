using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Compartidos;

public partial class Cargando
{
    [Parameter]
    public bool EnCurso { get; set; }
    [Parameter] public string Mensaje { get; set; } = "Cargando…";
    [Parameter]
    public string? Error { get; set; }
    [Parameter]
    public EventCallback AlReintentar { get; set; }
    [Parameter]
    public RenderFragment? ChildContent { get; set; }
}
