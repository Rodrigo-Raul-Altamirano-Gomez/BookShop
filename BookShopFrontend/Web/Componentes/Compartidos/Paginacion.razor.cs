using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Compartidos;

public partial class Paginacion
{
    [Parameter]
    public int Pagina { get; set; } = 1;

    [Parameter]
    public int TotalPaginas { get; set; }

    [Parameter]
    public EventCallback<int> PaginaCambiada { get; set; }
    private IEnumerable<int> PaginasVisibles
    {
        get
        {
            int primeraPagina = Math.Max(1, Pagina - 2);
            int ultimaPagina = Math.Min(TotalPaginas, Pagina + 2);
            var paginas = new List<int>();
            for (int numero = primeraPagina; numero <= ultimaPagina; numero++)
                paginas.Add(numero);
            return paginas;
        }
    }

}
