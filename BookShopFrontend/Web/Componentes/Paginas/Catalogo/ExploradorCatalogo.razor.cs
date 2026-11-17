using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class ExploradorCatalogo : IDisposable
{
    private string? errorCarga;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public string Titulo { get; set; } = "Catálogo";

    [Parameter]
    public string Busqueda { get; set; } = "";

    [Parameter]
    public string TipoAmbito { get; set; } = "";

    [Parameter]
    public string OrdenInicial { get; set; } = "MAS_RECIENTES";

    [Parameter]
    public int IdAmbito { get; set; }
    [Parameter]
    public bool? Favoritos { get; set; }
    [Parameter]
    public bool SoloNuevos { get; set; }
    [Parameter] public bool OrdenFijo { get; set; }
    [Parameter] public string? IdConsulta { get; set; }
    private long totalResultados;
    private List<Libro> resultados = [];
    private bool cargando = true;
    private VistaCatalogo vista = new();
    private bool precioFiltrado => vista.Minimo != 0 || vista.Maximo != 1000;
    private decimal LimitePrecio => Math.Max(1000, Math.Max(vista.Minimo, vista.Maximo));
    private string EstiloRango
    {
        get
        {
            string inicio = (vista.Minimo * 100 / LimitePrecio)
                .ToString(System.Globalization.CultureInfo.InvariantCulture);
            string fin = (vista.Maximo * 100 / LimitePrecio).ToString(System.Globalization.CultureInfo.InvariantCulture);
            return $"--inicio:{inicio}%;--fin:{fin}%";
        }
    }
    private IEnumerable<string> OpcionesCategorias =>
        Catalogo.Referencias.Where(r => r.Tipo == "categorias").Select(r => r.Nombre);
    private IEnumerable<string> OpcionesAutores =>
        Catalogo.Referencias.Where(r => r.Tipo == "autores").Select(r => r.Nombre);
    private IEnumerable<string> OpcionesEditoriales =>
        Catalogo.Referencias.Where(r => r.Tipo == "editoriales").Select(r => r.Nombre);
    private IEnumerable<string> OpcionesIdiomas => Catalogo.Idiomas;
    private bool HayFiltros => vista.Categorias.Count > 0 || vista.Autores.Count > 0
        || vista.Editoriales.Count > 0 || vista.Idiomas.Count > 0 || precioFiltrado;

    public static readonly Dictionary<string, string> OpcionesOrden = new()
    {
        ["MAS_RECIENTES"] = "Más recientes",
        ["MAS_VENDIDOS"] = "Más vendidos",
        ["PRECIO_ASCENDENTE"] = "Precio ascendente",
        ["PRECIO_DESCENDENTE"] = "Precio descendente",
        ["TITULO_ASCENDENTE"] = "Título A-Z",
        ["PAGINAS_ASCENDENTE"] = "Páginas ascendente",
        ["PAGINAS_DESCENDENTE"] = "Páginas descendente",
        ["STOCK_DESCENDENTE"] = "Stock descendente",
        ["ANIO_EDICION_DESCENDENTE"] = "Año de edición descendente"

    };
    private int TotalPaginas => Math.Max(1, (int)((totalResultados + 11) / 12));

    private async Task Consultar()
    {
        cargando = true;
        errorCarga = null;
        try
        {
            var filtro = new FiltroLibroDTO
            {
                Titulo = vista.Busqueda,
                Idiomas = vista.Idiomas.ToList(),
                PrecioMinimo = precioFiltrado ? vista.Minimo : null,
                PrecioMaximo = precioFiltrado ? vista.Maximo : null,
                SoloNuevos = SoloNuevos,
                Orden = OrdenFijo ? OrdenInicial : vista.Orden,
                Pagina = vista.Pagina,
                TamanioPagina = 12
            };
            foreach (var referencia in Catalogo.Referencias)
            {
                if (referencia.Tipo == "categorias" && vista.Categorias.Contains(referencia.Nombre))
                    filtro.IdsCategorias.Add(referencia.Id);
                if (referencia.Tipo == "autores" && vista.Autores.Contains(referencia.Nombre))
                    filtro.IdsAutores.Add(referencia.Id);
                if (referencia.Tipo == "editoriales" && vista.Editoriales.Contains(referencia.Nombre))
                    filtro.IdsEditoriales.Add(referencia.Id);
            }
            string ruta = "libros/busqueda";
            if (IdAmbito != 0)
            {
                string referencia = "editorial";
                if (TipoAmbito == "autores")
                    referencia = "autor";
                else if (TipoAmbito == "categorias")
                    referencia = "categoria";
                ruta = $"libros/por-{referencia}/{IdAmbito}";
            }
            if (Favoritos.HasValue)
                ruta = "cuenta/libros-guardados/busqueda?favorito="
                    + Favoritos.Value.ToString().ToLowerInvariant();
            if (IdConsulta != null)
                ruta = $"recomendaciones/consultas/{IdConsulta}/busqueda";
            var datos = await Api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>(ruta, filtro);
            resultados = datos.Items.Select(Presentacion.Libro).ToList();
            totalResultados = datos.TotalResultados;
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        cargando = false;
    }

    protected override async Task OnParametersSetAsync()
    {
        await Catalogo.Inicializar();
        var contexto = new Uri(Navegacion.Uri).AbsolutePath + "|" + OrdenInicial + "|"
            + TipoAmbito + "|" + Titulo + "|" + Favoritos + "|" + IdAmbito;
        if (Sesion.EstadoVistas.ContainsKey(contexto))
            vista = (VistaCatalogo)Sesion.EstadoVistas[contexto];
        else
        {
            vista = new VistaCatalogo { Orden = OrdenInicial };
            Sesion.EstadoVistas[contexto] = vista;
        }
        if (vista.BusquedaUrl != Busqueda)
        {
            vista.Busqueda = Busqueda;
            vista.BusquedaUrl = Busqueda;
            vista.Pagina = 1;
        }
        Sesion.MostrarBusqueda(vista.Busqueda, "Buscar por título", Buscar);
        await Consultar();
    }

    private async Task Buscar(string texto)
    {
        vista.Busqueda = texto;
        await InvokeAsync(async () => { await ActualizarResultados(); StateHasChanged(); });
    }
    public void Dispose()
    {
        if (Sesion.BuscarEnListado == Buscar) Sesion.BuscarEnListado = null;
    }

    // Conserva únicamente los controles de pantalla al volver desde un libro.
    private class VistaCatalogo
    {
        // Permite volver del detalle sin reemplazar la búsqueda del encabezado por un q anterior de la URL.
        public string BusquedaUrl { get; set; } = "";
        public string Busqueda { get; set; } = "";
        public HashSet<string> Categorias { get; set; } = [];
        public HashSet<string> Autores { get; set; } = [];
        public HashSet<string> Editoriales { get; set; } = [];
        public HashSet<string> Idiomas { get; set; } = [];
        public decimal Minimo { get; set; }
        public decimal Maximo { get; set; } = 1000;
        public string Orden { get; set; } = "MAS_RECIENTES";
        public int Pagina { get; set; } = 1;
    }

    private async Task SeleccionarFiltro(HashSet<string> seleccionados, string valor)
    {
        if (seleccionados.Contains(valor))
            seleccionados.Remove(valor);
        else
            seleccionados.Add(valor);
        await ActualizarResultados();
    }

    private async Task AjustarMinimo()
    {
        vista.Minimo = Math.Max(0, vista.Minimo);
        if (vista.Minimo > vista.Maximo)
            vista.Maximo = vista.Minimo;
        await ActualizarResultados();
    }

    private async Task AjustarMaximo()
    {
        vista.Maximo = Math.Max(0, vista.Maximo);
        if (vista.Maximo < vista.Minimo)
            vista.Minimo = vista.Maximo;
        await ActualizarResultados();
    }

    private async Task QuitarPrecio()
    {
        vista.Minimo = 0;
        vista.Maximo = 1000;
        await ActualizarResultados();
    }

    private async Task ActualizarResultados()
    {
        vista.Pagina = 1;
        await Consultar();
    }

    private async Task CambiarPagina(int valor)
    {
        vista.Pagina = valor;
        await Consultar();
    }
}
