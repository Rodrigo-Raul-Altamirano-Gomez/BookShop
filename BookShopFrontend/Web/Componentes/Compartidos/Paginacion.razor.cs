using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Compartidos;

public partial class Paginacion
{
    [Parameter]
    public int Pagina { get; set; } = 1;

    [Parameter]
    public int TotalPaginas
    {
        get; set;
    }

    [Parameter]
    public EventCallback<int> PaginaCambiada
    {
        get; set;
    }
    private IEnumerable<int> PaginasVisibles => Enumerable.Range(Math.Max(1, Pagina - 2), Math.Max(0, Math
        .Min(TotalPaginas, Pagina + 2) - Math.Max(1, Pagina - 2) + 1));

    private Task Ir(int page) => PaginaCambiada.InvokeAsync(page);
}

