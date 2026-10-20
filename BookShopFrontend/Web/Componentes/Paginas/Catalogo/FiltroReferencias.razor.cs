using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Catalogo;

public partial class FiltroReferencias
{
    [Parameter]
    public string Titulo { get; set; } = "";

    [Parameter]
    public string Singular { get; set; } = "";

    [Parameter]
    public IEnumerable<string> Opciones { get; set; } = [];

    [Parameter]
    public HashSet<string> Seleccionados { get; set; } = [];

    [Parameter]
    public EventCallback<string> SeleccionCambiada
    {
        get; set;
    }

    private string textoBusqueda = "", busquedaAplicada = "";
    private bool expandido;
    private List<string> OpcionesFiltradas => Opciones.Where(o => o.Contains(busquedaAplicada,
         StringComparison.OrdinalIgnoreCase)).Distinct().Order().ToList();
    private IEnumerable<string> VisibleOptions => expandido ? OpcionesFiltradas : OpcionesFiltradas.Take(4);

    private void Buscar()
    {
        busquedaAplicada = textoBusqueda.Trim();
        expandido = false;
    }

    private void AlternarExpansion() => expandido = !expandido;
    private Task Seleccionar(string option) => SeleccionCambiada.InvokeAsync(option);
}

