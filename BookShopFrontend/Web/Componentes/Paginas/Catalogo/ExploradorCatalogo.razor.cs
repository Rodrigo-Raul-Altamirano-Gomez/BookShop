using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class ExploradorCatalogo : IDisposable
{
    private string? errorCarga;

    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public IEnumerable<Libro> Libros { get; set; } = [];

    [Parameter]
    public string Titulo { get; set; } = "Catálogo";

    [Parameter]
    public string Busqueda { get; set; } = "";

    [Parameter]
    public string TipoAmbito { get; set; } = "";

    [Parameter]
    public string OrdenInicial { get; set; } = "reciente";

    [Parameter]
    public bool Administracion
    {
        get; set;
    }

    [Parameter]
    public RenderFragment? AccionesCabecera
    {
        get; set;
    }

    [Parameter]
    public int IdAmbito
    {
        get; set;
    }
    [Parameter]
    public bool? Favoritos
    {
        get; set;
    }
    [Parameter]
    public bool SoloNuevos
    {
        get; set;
    }
    private long totalResultados;
    private List<Libro> resultados = [];
    private bool cargando = true;
    private HashSet<string> categorias = [], autores = [], editoriales = [], idiomas = [];
    private HashSet<string> categoriasAplicadas = [], autoresAplicados = [], editorialesAplicadas = [],
         idiomasAplicados = [];
    private decimal minimo, minimoAplicado;
    private decimal maximo, maximoAplicado, limiteAnterior;
    private bool precioFiltrado;
    private bool soloActivos = true, soloActivosAplicado = true;
    private string orden = "reciente";
    private int pagina = 1;
    private string contextoAnterior = "";
    private decimal LimitePrecio => 1000;
    private string EstiloRango => ($"--inicio:" + $"{(minimo * 100 / LimitePrecio).ToString(System
        .Globalization.CultureInfo.InvariantCulture)}%;--fin:" + $"{(maximo * 100 / LimitePrecio).ToString(
            System.Globalization.CultureInfo.InvariantCulture)}%");
    private IEnumerable<string> OpcionesCategorias => Catalogo.Referencias.Where(r => r.Tipo == "categorias").Select(r => r.Nombre);
    private IEnumerable<string> OpcionesAutores => Catalogo.Referencias.Where(r => r.Tipo == "autores").Select(r => r.Nombre);
    private IEnumerable<string> OpcionesEditoriales => Catalogo.Referencias.Where(r => r.Tipo == "editoriales").Select(r => r.Nombre);
    private IEnumerable<string> OpcionesIdiomas => Catalogo.Idiomas;
    private bool HayFiltros => categoriasAplicadas.Count + autoresAplicados.Count + editorialesAplicadas
        .Count + idiomasAplicados.Count > 0 || precioFiltrado || Administracion && soloActivosAplicado;

    public static readonly Dictionary<string, string> OpcionesOrden = new()
    {

        ["reciente"] = "Más recientes",

        ["vendidos"] = "Más vendidos",

        ["precio-asc"] = "Precio ascendente",

        ["precio-desc"] = "Precio descendente",

        ["titulo"] = "Título A-Z",

        ["paginas-asc"] = "Páginas ascendente",

        ["paginas-desc"] = "Páginas descendente",

        ["stock"] = "Stock descendente",

        ["ano"] = "Año de edición descendente"

    };
    private List<Libro> Resultados => resultados;
    private int TotalPaginas => Math.Max(1, (int)((totalResultados + 11) / 12));
    private IEnumerable<Libro> LibrosVisibles => resultados;

    private async Task Consultar()
    {
        cargando = true;
        errorCarga = null;
        try
        {


            var filtro = new FiltroLibroDTO
            {
                Titulo = Busqueda,
                IdsCategorias = Catalogo.Referencias.Where(r => r.Tipo == "categorias" && categoriasAplicadas.Contains(r.Nombre)).Select(r => r.Id).ToList(),
                IdsAutores = Catalogo.Referencias.Where(r => r.Tipo == "autores" && autoresAplicados.Contains(r.Nombre)).Select(r => r.Id).ToList(),
                IdsEditoriales = Catalogo.Referencias.Where(r => r.Tipo == "editoriales" && editorialesAplicadas.Contains(r.Nombre)).Select(r => r.Id).ToList(),
                Idiomas = idiomasAplicados.ToList(),
                PrecioMinimo = precioFiltrado ? minimoAplicado : null,
                PrecioMaximo = precioFiltrado ? maximoAplicado : null,
                SoloActivos = true,
                SoloNuevos = SoloNuevos,
                Orden = orden switch
                {
                    "vendidos" => "MAS_VENDIDOS",
                    "precio-asc" => "PRECIO_ASCENDENTE",
                    "precio-desc" => "PRECIO_DESCENDENTE",
                    "titulo" => "TITULO_ASCENDENTE",
                    "paginas-asc" => "PAGINAS_ASCENDENTE",
                    "paginas-desc" => "PAGINAS_DESCENDENTE",
                    "stock" => "STOCK_DESCENDENTE",
                    "ano" => "ANIO_EDICION_DESCENDENTE",
                    _ => "MAS_RECIENTES"
                },
                Pagina = pagina,
                TamanioPagina = 12
            };
            string ruta = IdAmbito == 0 ? "libros/busqueda" : $"libros/por-{(TipoAmbito == "autores" ? "autor" : TipoAmbito == "categorias" ? "categoria" : "editorial")}/{IdAmbito}";
            if (Favoritos.HasValue)
                ruta = $"cuenta/libros-guardados/busqueda?favorito={Favoritos.Value.ToString().ToLowerInvariant()}";
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
                categoriasAplicadas = [.. vista.CategoriasAplicadas];
                autoresAplicados = [.. vista.AutoresAplicados];
                editorialesAplicadas = [.. vista.EditorialesAplicadas];
                idiomasAplicados = [.. vista.IdiomasAplicados];
                minimo = Math.Clamp(vista.Minimo, 0, LimitePrecio);
                maximo = vista.Maximo == vista.Limite ? LimitePrecio : Math.Clamp(vista.Maximo, minimo,
                     LimitePrecio);
                precioFiltrado = vista.PrecioFiltrado;
                minimoAplicado = Math.Clamp(vista.MinimoAplicado, 0, LimitePrecio);
                maximoAplicado = precioFiltrado ? Math.Clamp(vista.MaximoAplicado, minimoAplicado,
                     LimitePrecio) : LimitePrecio;
                soloActivos = vista.SoloActivos;
                soloActivosAplicado = vista.SoloActivosAplicado;
                orden = vista.Orden;
                pagina = vista.Pagina;
            }
            else
            {
                categorias = [];
                autores = [];
                editoriales = [];
                idiomas = [];
                categoriasAplicadas = [];
                autoresAplicados = [];
                editorialesAplicadas = [];
                idiomasAplicados = [];
                minimo = minimoAplicado = 0;
                maximo = maximoAplicado = LimitePrecio;
                precioFiltrado = false;
                soloActivos = soloActivosAplicado = true;
                pagina = 1;
                orden = OrdenInicial;
            }
        }
        else if (limiteAnterior != LimitePrecio)
        {
            if (maximo == limiteAnterior)
                maximo = LimitePrecio;
            minimo = Math.Max(0, minimo);
            maximo = Math.Clamp(maximo, minimo, LimitePrecio);
            if (!precioFiltrado)
                maximoAplicado = LimitePrecio;
            else
            {
                minimoAplicado = Math.Clamp(minimoAplicado, 0, LimitePrecio);
                maximoAplicado = Math.Clamp(maximoAplicado, minimoAplicado, LimitePrecio);
            }
        }

        limiteAnterior = LimitePrecio;
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

            CategoriasAplicadas = [.. categoriasAplicadas],

            AutoresAplicados = [.. autoresAplicados],

            EditorialesAplicadas = [.. editorialesAplicadas],

            IdiomasAplicados = [.. idiomasAplicados],

            Minimo = minimo,

            Maximo = maximo,

            MinimoAplicado = minimoAplicado,

            MaximoAplicado = maximoAplicado,

            Limite = limiteAnterior,

            PrecioFiltrado = precioFiltrado,

            SoloActivos = soloActivos,

            SoloActivosAplicado = soloActivosAplicado,

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
        public HashSet<string> CategoriasAplicadas { get; set; } = [];
        public HashSet<string> AutoresAplicados { get; set; } = [];
        public HashSet<string> EditorialesAplicadas { get; set; } = [];
        public HashSet<string> IdiomasAplicados { get; set; } = [];
        public decimal Minimo
        {
            get; set;
        }
        public decimal Maximo
        {
            get; set;
        }
        public decimal MinimoAplicado
        {
            get; set;
        }
        public decimal MaximoAplicado
        {
            get; set;
        }
        public decimal Limite
        {
            get; set;
        }
        public bool PrecioFiltrado
        {
            get; set;
        }
        public bool SoloActivos
        {
            get; set;
        }
        public bool SoloActivosAplicado
        {
            get; set;
        }
        public string Orden { get; set; } = "reciente";
        public int Pagina { get; set; } = 1;
    }

    private void SeleccionarCategoria(string value)
    {
        if (!categorias.Add(value))
            categorias.Remove(value);
    }

    private void SeleccionarAutor(string value)
    {
        if (!autores.Add(value))
            autores.Remove(value);
    }

    private void SeleccionarEditorial(string value)
    {
        if (!editoriales.Add(value))
            editoriales.Remove(value);
    }

    private void SeleccionarIdioma(string value)
    {
        if (!idiomas.Add(value))
            idiomas.Remove(value);
    }

    private void AjustarMinimo()
    {
        minimo = Math.Clamp(minimo, 0, LimitePrecio);
        if (minimo > maximo)
            maximo = minimo;
    }

    private void AjustarMaximo()
    {
        maximo = Math.Max(0, maximo);
        if (maximo < minimo)
            minimo = maximo;
    }

    private async Task AplicarFiltros()
    {
        categoriasAplicadas = [.. categorias];
        autoresAplicados = [.. autores];
        editorialesAplicadas = [.. editoriales];
        idiomasAplicados = [.. idiomas];
        minimoAplicado = minimo;
        maximoAplicado = maximo;
        precioFiltrado = minimo != 0 || maximo != LimitePrecio;
        soloActivosAplicado = soloActivos;
        pagina = 1;
        await Consultar();
    }

    private async Task LimpiarFiltros()
    {
        categorias.Clear();
        autores.Clear();
        editoriales.Clear();
        idiomas.Clear();
        minimo = 0;
        maximo = LimitePrecio;
        soloActivos = false;
        await AplicarFiltros();
    }

    private async Task QuitarCategoria(string value)
    {
        categorias.Remove(value);
        categoriasAplicadas.Remove(value);
        pagina = 1;
        await Consultar();
    }

    private async Task QuitarAutor(string value)
    {
        autores.Remove(value);
        autoresAplicados.Remove(value);
        pagina = 1;
        await Consultar();
    }

    private async Task QuitarEditorial(string value)
    {
        editoriales.Remove(value);
        editorialesAplicadas.Remove(value);
        pagina = 1;
        await Consultar();
    }

    private async Task QuitarIdioma(string value)
    {
        idiomas.Remove(value);
        idiomasAplicados.Remove(value);
        pagina = 1;
        await Consultar();
    }

    private async Task QuitarPrecio()
    {
        minimo = minimoAplicado = 0;
        maximo = maximoAplicado = LimitePrecio;
        precioFiltrado = false;
        pagina = 1;
        await Consultar();
    }

    private async Task QuitarSoloActivos()
    {
        soloActivos = soloActivosAplicado = false;
        pagina = 1;
        await Consultar();
    }

    private async Task CambiarOrden()
    {
        pagina = 1;
        await Consultar();
    }
    private async Task CambiarPagina(int value)
    {
        pagina = value;
        await Consultar();
    }
}

