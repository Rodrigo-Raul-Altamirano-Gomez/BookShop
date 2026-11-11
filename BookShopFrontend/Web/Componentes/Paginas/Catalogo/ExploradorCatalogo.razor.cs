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
    private HashSet<string> categoriasAplicadas = [], autoresAplicados = [], editorialesAplicadas = [],
         idiomasAplicados = [];
    private decimal minimo, minimoAplicado;
    private decimal maximo, maximoAplicado;
    private bool precioFiltrado;
    private string orden = "MAS_RECIENTES";
    private int pagina = 1;
    private string contextoAnterior = "";
    private const decimal LimitePrecio = 1000;
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
    private bool HayFiltros => categoriasAplicadas.Count > 0 || autoresAplicados.Count > 0
        || editorialesAplicadas.Count > 0 || idiomasAplicados.Count > 0 || precioFiltrado;

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
                Idiomas = idiomasAplicados.ToList(),
                PrecioMinimo = precioFiltrado ? minimoAplicado : null,
                PrecioMaximo = precioFiltrado ? maximoAplicado : null,
                SoloNuevos = SoloNuevos,
                Orden = orden,
                Pagina = pagina,
                TamanioPagina = 12
            };
            foreach (var referencia in Catalogo.Referencias)
            {
                if (referencia.Tipo == "categorias" && categoriasAplicadas.Contains(referencia.Nombre))
                    filtro.IdsCategorias.Add(referencia.Id);
                if (referencia.Tipo == "autores" && autoresAplicados.Contains(referencia.Nombre))
                    filtro.IdsAutores.Add(referencia.Id);
                if (referencia.Tipo == "editoriales" && editorialesAplicadas.Contains(referencia.Nombre))
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
                categoriasAplicadas = [.. vista.CategoriasAplicadas];
                autoresAplicados = [.. vista.AutoresAplicados];
                editorialesAplicadas = [.. vista.EditorialesAplicadas];
                idiomasAplicados = [.. vista.IdiomasAplicados];
                minimo = vista.Minimo;
                maximo = vista.Maximo;
                precioFiltrado = vista.PrecioFiltrado;
                minimoAplicado = vista.MinimoAplicado;
                maximoAplicado = vista.MaximoAplicado;
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
            CategoriasAplicadas = [.. categoriasAplicadas],
            AutoresAplicados = [.. autoresAplicados],
            EditorialesAplicadas = [.. editorialesAplicadas],
            IdiomasAplicados = [.. idiomasAplicados],
            Minimo = minimo,
            Maximo = maximo,
            MinimoAplicado = minimoAplicado,
            MaximoAplicado = maximoAplicado,
            PrecioFiltrado = precioFiltrado,
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
        public decimal Minimo { get; set; }
        public decimal Maximo { get; set; }
        public decimal MinimoAplicado { get; set; }
        public decimal MaximoAplicado { get; set; }
        public bool PrecioFiltrado { get; set; }
        public string Orden { get; set; } = "MAS_RECIENTES";
        public int Pagina { get; set; } = 1;
    }

    private void SeleccionarCategoria(string valor)
    {
        if (!categorias.Add(valor))
            categorias.Remove(valor);
    }

    private void SeleccionarAutor(string valor)
    {
        if (!autores.Add(valor))
            autores.Remove(valor);
    }

    private void SeleccionarEditorial(string valor)
    {
        if (!editoriales.Add(valor))
            editoriales.Remove(valor);
    }

    private void SeleccionarIdioma(string valor)
    {
        if (!idiomas.Add(valor))
            idiomas.Remove(valor);
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
        await AplicarFiltros();
    }

    private async Task QuitarCategoria(string valor)
    {
        categorias.Remove(valor);
        categoriasAplicadas.Remove(valor);
        pagina = 1;
        await Consultar();
    }

    private async Task QuitarAutor(string valor)
    {
        autores.Remove(valor);
        autoresAplicados.Remove(valor);
        pagina = 1;
        await Consultar();
    }

    private async Task QuitarEditorial(string valor)
    {
        editoriales.Remove(valor);
        editorialesAplicadas.Remove(valor);
        pagina = 1;
        await Consultar();
    }

    private async Task QuitarIdioma(string valor)
    {
        idiomas.Remove(valor);
        idiomasAplicados.Remove(valor);
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

    private async Task CambiarOrden()
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
