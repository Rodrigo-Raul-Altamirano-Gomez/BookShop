using System.Globalization;
using BookShopWeb.Code;
using BookShopWeb.Models;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Routing;
using Microsoft.AspNetCore.WebUtilities;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Shared;

public partial class ResultsView : IDisposable
{
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private IJSRuntime JS { get; set; } = default!;

    // Ruta del API que devuelve la vista (por ejemplo "libros" o "libros/novedades")
    [Parameter, EditorRequired] public string Endpoint { get; set; } = "";
    [Parameter] public bool PermiteOrden { get; set; } = true;
    [Parameter] public string CriterioOrden { get; set; } = "";
    // "registro" o "ventas": cambia la ayuda del filtro de período
    [Parameter] public string SignificadoPeriodo { get; set; } = "registro";
    // En la página de una entidad se oculta su propio filtro
    [Parameter] public TipoEntidad? FiltroOculto { get; set; }
    [Parameter] public bool PermiteQuitar { get; set; }
    [Parameter] public EventCallback<Libro> OnQuitar { get; set; }
    [Parameter] public EventCallback<int> OnCargado { get; set; }
    // Vacío sin filtros (por ejemplo, una lista sin libros)
    [Parameter] public string? TituloVacio { get; set; }
    [Parameter] public string? TextoVacio { get; set; }
    [Parameter] public string? AccionVacia { get; set; }
    [Parameter] public string? UrlVacia { get; set; }

    private ResultadoLibros? resultado;
    private bool cargando = true;
    private string ruta = "";
    private string? endpointCargado;

    // Estado de los filtros, leído de la URL
    private string? q;
    private List<int> categorias = new();
    private List<int> autores = new();
    private List<int> editoriales = new();
    private string? idioma;
    private decimal? min;
    private decimal? max;
    private DateOnly? desde;
    private DateOnly? hasta;
    private string orden = "titulo-asc";
    private int pagina = 1;

    private bool HayFiltros => !string.IsNullOrEmpty(q) || categorias.Count > 0 || autores.Count > 0 || editoriales.Count > 0
                               || !string.IsNullOrEmpty(idioma) || min != null || max != null || desde != null || hasta != null;

    protected override void OnInitialized()
    {
        ruta = RutaActual();
        Navegacion.LocationChanged += AlCambiarUrl;
    }

    protected override async Task OnParametersSetAsync()
    {
        if (endpointCargado != Endpoint)
        {
            endpointCargado = Endpoint;
            await Cargar();
        }
    }

    // Atrás, Adelante o un cambio de filtro: si sigue en la misma página, se recarga con la nueva URL
    private async void AlCambiarUrl(object? sender, LocationChangedEventArgs e)
    {
        if (RutaActual() != ruta) return;
        await InvokeAsync(async () =>
        {
            await Cargar();
            StateHasChanged();
        });
    }

    private async Task Cargar()
    {
        cargando = true;
        string consulta = new Uri(Navegacion.Uri).Query;
        LeerUrl(consulta);
        // Los parámetros de la URL tienen los mismos nombres que los del API
        resultado = await new HttpClientUtils<ResultadoLibros>().Get(Endpoint + consulta);
        cargando = false;
        await OnCargado.InvokeAsync(resultado!.Total);
    }

    private void LeerUrl(string consulta)
    {
        var parametros = QueryHelpers.ParseQuery(consulta);
        q = Texto(parametros, "q");
        categorias = Numeros(parametros, "categoria");
        autores = Numeros(parametros, "autor");
        editoriales = Numeros(parametros, "editorial");
        idioma = Texto(parametros, "idioma");
        min = decimal.TryParse(Texto(parametros, "min"), NumberStyles.Number, CultureInfo.InvariantCulture, out decimal valorMin) ? valorMin : null;
        max = decimal.TryParse(Texto(parametros, "max"), NumberStyles.Number, CultureInfo.InvariantCulture, out decimal valorMax) ? valorMax : null;
        desde = DateOnly.TryParse(Texto(parametros, "desde"), CultureInfo.InvariantCulture, out DateOnly fechaDesde) ? fechaDesde : null;
        hasta = DateOnly.TryParse(Texto(parametros, "hasta"), CultureInfo.InvariantCulture, out DateOnly fechaHasta) ? fechaHasta : null;
        orden = Texto(parametros, "orden") ?? "titulo-asc";
        pagina = int.TryParse(Texto(parametros, "pagina"), out int numero) ? numero : 1;
    }

    // Cualquier cambio de filtro vuelve a la página 1 (ERS §2.3)
    private void Navegar()
    {
        pagina = 1;
        IrAUrl();
    }

    private async Task CambiarPagina(int nueva)
    {
        pagina = nueva;
        IrAUrl();
        await JS.InvokeVoidAsync("irArriba");
    }

    private void CambiarSeleccion(List<int> lista, List<int> nuevas)
    {
        lista.Clear();
        lista.AddRange(nuevas);
        Navegar();
    }

    private void LimpiarTodo()
    {
        q = null;
        categorias.Clear();
        autores.Clear();
        editoriales.Clear();
        idioma = null;
        min = null;
        max = null;
        desde = null;
        hasta = null;
        Navegar();
    }

    // Arma la URL con el estado actual; se conservan los parámetros propios de la página (por ejemplo, ?lista=)
    private void IrAUrl()
    {
        var actuales = QueryHelpers.ParseQuery(new Uri(Navegacion.Uri).Query);
        string[] propios = { "q", "categoria", "autor", "editorial", "idioma", "min", "max", "desde", "hasta", "orden", "pagina" };
        List<string> partes = new();
        foreach (var parametro in actuales)
        {
            if (!propios.Contains(parametro.Key))
            {
                partes.Add(parametro.Key + "=" + Uri.EscapeDataString(parametro.Value.ToString()));
            }
        }
        if (!string.IsNullOrWhiteSpace(q)) partes.Add("q=" + Uri.EscapeDataString(q.Trim()));
        foreach (int id in categorias) partes.Add("categoria=" + id);
        foreach (int id in autores) partes.Add("autor=" + id);
        foreach (int id in editoriales) partes.Add("editorial=" + id);
        if (!string.IsNullOrEmpty(idioma)) partes.Add("idioma=" + Uri.EscapeDataString(idioma));
        if (min != null) partes.Add("min=" + min.Value.ToString(CultureInfo.InvariantCulture));
        if (max != null) partes.Add("max=" + max.Value.ToString(CultureInfo.InvariantCulture));
        if (desde != null) partes.Add("desde=" + desde.Value.ToString("yyyy-MM-dd"));
        if (hasta != null) partes.Add("hasta=" + hasta.Value.ToString("yyyy-MM-dd"));
        if (PermiteOrden && orden != "titulo-asc") partes.Add("orden=" + orden);
        if (pagina > 1) partes.Add("pagina=" + pagina);
        Navegacion.NavigateTo(ruta + (partes.Count > 0 ? "?" + string.Join("&", partes) : ""));
    }

    // Un chip por cada valor elegido en los filtros de casillas
    private List<(string Grupo, string Valor, string Texto, Action Quitar)> Chips()
    {
        var chips = new List<(string, string, string, Action)>();
        AgregarChips(chips, "Categoría", categorias, resultado!.Categorias);
        AgregarChips(chips, "Autor", autores, resultado.Autores);
        AgregarChips(chips, "Editorial", editoriales, resultado.Editoriales);
        return chips;
    }

    private void AgregarChips(List<(string, string, string, Action)> chips, string grupo, List<int> elegidas, List<Entidad> opciones)
    {
        foreach (int id in elegidas)
        {
            Entidad? opcion = opciones.FirstOrDefault(o => o.Id == id);
            if (opcion == null) continue;
            chips.Add((grupo, opcion.Nombre, grupo + ": " + opcion.Nombre, () =>
            {
                elegidas.Remove(id);
                Navegar();
            }));
        }
    }

    private string RutaActual() => "/" + Navegacion.ToBaseRelativePath(Navegacion.Uri).Split('?')[0];

    private static string? Texto(Dictionary<string, Microsoft.Extensions.Primitives.StringValues> parametros, string clave)
    {
        return parametros.TryGetValue(clave, out var valor) && valor.ToString() != "" ? valor.ToString() : null;
    }

    private static List<int> Numeros(Dictionary<string, Microsoft.Extensions.Primitives.StringValues> parametros, string clave)
    {
        List<int> numeros = new();
        if (parametros.TryGetValue(clave, out var valores))
        {
            foreach (string? valor in valores)
            {
                if (int.TryParse(valor, out int numero)) numeros.Add(numero);
            }
        }
        return numeros;
    }

    public void Dispose()
    {
        Navegacion.LocationChanged -= AlCambiarUrl;
    }
}
