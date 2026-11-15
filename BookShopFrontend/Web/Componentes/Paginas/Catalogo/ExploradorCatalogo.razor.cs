using Microsoft.AspNetCore.Components;
using Web.Servicios;
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
    private long totalResultados;
    private List<Libro> resultados = [];
    private bool cargando = true;
    private HashSet<string> categorias = [], autores = [], editoriales = [], idiomas = [];
    private decimal minimo;
    private decimal maximo;
    private bool precioFiltrado => minimo != 0 || maximo != 1000;
    private string orden = "MAS_RECIENTES";
    private int pagina = 1;
    private string contextoAnterior = "";
    private decimal LimitePrecio => Math.Max(1000, Math.Max(minimo, maximo));
    private string EstiloRango
    {
        get
        {
            string inicio = (minimo * 100 / LimitePrecio)
                .ToString(System.Globalization.CultureInfo.InvariantCulture);
            string fin = (maximo * 100 / LimitePrecio).ToString(System.Globalization.CultureInfo.InvariantCulture);
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
    private bool HayFiltros => categorias.Count > 0 || autores.Count > 0
        || editoriales.Count > 0 || idiomas.Count > 0 || precioFiltrado;

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
                Titulo = Busqueda,
                Idiomas = idiomas.ToList(),
                PrecioMinimo = precioFiltrado ? minimo : null,
                PrecioMaximo = precioFiltrado ? maximo : null,
                SoloNuevos = SoloNuevos,
                Orden = orden,
                Pagina = pagina,
                TamanioPagina = 12
            };
            foreach (var referencia in Catalogo.Referencias)
            {
                if (referencia.Tipo == "categorias" && categorias.Contains(referencia.Nombre))
                    filtro.IdsCategorias.Add(referencia.Id);
                if (referencia.Tipo == "autores" && autores.Contains(referencia.Nombre))
                    filtro.IdsAutores.Add(referencia.Id);
                if (referencia.Tipo == "editoriales" && editoriales.Contains(referencia.Nombre))
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
            var datos = await Api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>(ruta, filtro);
            resultados = datos.Items.Select(Presentacion.Libro).ToList();
            totalResultados = datos.TotalResultados;
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        finally
        {
            cargando = false;
        }
    }

    protected override async Task OnParametersSetAsync()
    {
        await Catalogo.Inicializar();
        var contexto = Navegacion.ToBaseRelativePath(Navegacion.Uri).Split('#')[0] + "|" + Busqueda + "|"
            + TipoAmbito + "|" + Titulo + "|" + Favoritos + "|" + IdAmbito;
        if (contexto != contextoAnterior)
        {
            GuardarVista();
            contextoAnterior = contexto;
            if (Sesion.EstadoVistas.TryGetValue(contexto, out var guardado) && guardado is VistaCatalogo vista)
            {
                categorias = [.. vista.Categorias];
                autores = [.. vista.Autores];
                editoriales = [.. vista.Editoriales];
                idiomas = [.. vista.Idiomas];
                minimo = vista.Minimo;
                maximo = vista.Maximo;
                orden = vista.Orden;
                pagina = vista.Pagina;
            }
            else
            {
                categorias = [];
                autores = [];
                editoriales = [];
                idiomas = [];
                minimo = 0;
                maximo = 1000;
                pagina = 1;
                orden = OrdenInicial;
            }
        }
        await Consultar();
    }

    private void GuardarVista()
    {
        if (contextoAnterior.Length == 0)
            return;
        Sesion.EstadoVistas[contextoAnterior] = new VistaCatalogo
        {
            Categorias = [.. categorias],
            Autores = [.. autores],
            Editoriales = [.. editoriales],
            Idiomas = [.. idiomas],
            Minimo = minimo,
            Maximo = maximo,
            Orden = orden,
            Pagina = pagina

        };
    }

    public void Dispose() => GuardarVista();
    // Conserva únicamente los controles de pantalla al volver desde un libro.
    private class VistaCatalogo
    {
        public HashSet<string> Categorias { get; set; } = [];
        public HashSet<string> Autores { get; set; } = [];
        public HashSet<string> Editoriales { get; set; } = [];
        public HashSet<string> Idiomas { get; set; } = [];
        public decimal Minimo { get; set; }
        public decimal Maximo { get; set; }
        public string Orden { get; set; } = "MAS_RECIENTES";
        public int Pagina { get; set; } = 1;
    }

    private async Task SeleccionarCategoria(string valor)
    {
        if (!categorias.Add(valor))
            categorias.Remove(valor);
        await ActualizarResultados();
    }

    private async Task SeleccionarAutor(string valor)
    {
        if (!autores.Add(valor))
            autores.Remove(valor);
        await ActualizarResultados();
    }

    private async Task SeleccionarEditorial(string valor)
    {
        if (!editoriales.Add(valor))
            editoriales.Remove(valor);
        await ActualizarResultados();
    }

    private async Task SeleccionarIdioma(string valor)
    {
        if (!idiomas.Add(valor))
            idiomas.Remove(valor);
        await ActualizarResultados();
    }

    private async Task AjustarMinimo()
    {
        minimo = Math.Max(0, minimo);
        if (minimo > maximo)
            maximo = minimo;
        await ActualizarResultados();
    }

    private async Task AjustarMaximo()
    {
        maximo = Math.Max(0, maximo);
        if (maximo < minimo)
            minimo = maximo;
        await ActualizarResultados();
    }

    private async Task QuitarPrecio()
    {
        minimo = 0;
        maximo = 1000;
        await ActualizarResultados();
    }

    private async Task ActualizarResultados()
    {
        pagina = 1;
        await Consultar();
    }

    private async Task CambiarPagina(int valor)
    {
        pagina = valor;
        await Consultar();
    }
}
