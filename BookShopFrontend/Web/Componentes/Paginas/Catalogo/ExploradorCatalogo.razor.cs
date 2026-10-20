using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class ExploradorCatalogo : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

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
    private decimal LimitePrecio => Math.Max(10, Math.Ceiling(Libros.Select(b => b.Precio).DefaultIfEmpty(
        0).Max() / 10) * 10);
    private string EstiloRango => ($"--inicio:" + $"{(minimo * 100 / LimitePrecio).ToString(System
        .Globalization.CultureInfo.InvariantCulture)}%;--fin:" + $"{(maximo * 100 / LimitePrecio).ToString(
            System.Globalization.CultureInfo.InvariantCulture)}%");
    private IEnumerable<string> OpcionesCategorias => Libros.Select(b => b.Categoria);
    private IEnumerable<string> OpcionesAutores => Libros.Select(b => b.Autor);
    private IEnumerable<string> OpcionesEditoriales => Libros.Select(b => b.Editorial);
    private IEnumerable<string> OpcionesIdiomas => Libros.Select(b => b.Idioma);
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
    private List<Libro> Resultados
    {
        get
        {
            var query = Libros.Where(b => (Administracion ? !soloActivosAplicado || b.Activo : b.Activo)
                && b.Titulo.Contains(Busqueda, StringComparison.OrdinalIgnoreCase) && (categoriasAplicadas
                    .Count == 0 || categoriasAplicadas.Contains(b.Categoria)) && (autoresAplicados
                        .Count == 0 || autoresAplicados.Contains(b.Autor)) && (editorialesAplicadas
                        .Count == 0 || editorialesAplicadas.Contains(b.Editorial)) && (idiomasAplicados
                        .Count == 0 || idiomasAplicados.Contains(b.Idioma)) && (!precioFiltrado || b
                        .Precio >= minimoAplicado && b.Precio <= maximoAplicado));
            return OrdenarLibros(query, orden).ToList();
        }
    }

    private int TotalPaginas => Math.Max(1, (Resultados.Count + 11) / 12);
    private IEnumerable<Libro> LibrosVisibles => Resultados.Skip((Math.Min(pagina, TotalPaginas) - 1) * 12)
        .Take(12);

    protected override void OnParametersSet()
    {
        var contexto = Navegacion.ToBaseRelativePath(Navegacion.Uri).Split('#')[0] + "|" + Busqueda + "|"
            + TipoAmbito + "|" + Titulo;
        if (contexto != contextoAnterior)
        {
            GuardarVista();
            contextoAnterior = contexto;
            if (Estado.EstadoVistas.TryGetValue(contexto, out var guardado) && guardado is VistaCatalogo vista)
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
            minimo = Math.Clamp(minimo, 0, LimitePrecio);
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
        pagina = Math.Min(pagina, TotalPaginas);
    }

    private void GuardarVista()
    {
        if (contextoAnterior.Length == 0)
            return;
        Estado.EstadoVistas[contextoAnterior] = new VistaCatalogo
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

    public static IEnumerable<Libro> OrdenarLibros(IEnumerable<Libro> books, string order) => order switch
    {
        "reciente" => books.OrderByDescending(b => b.Id),
        "precio-asc" => books.OrderBy(b => b.Precio),
        "precio-desc" => books.OrderByDescending(b => b.Precio),
        "titulo" => books.OrderBy(b => b.Titulo),
        "paginas-asc" => books.OrderBy(b => b.Paginas),
        "paginas-desc" => books.OrderByDescending(b => b.Paginas),
        "stock" => books.OrderByDescending(b => b.Stock),
        "ano" => books.OrderByDescending(b => b.Anio),
        _ => books
    };
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
        maximo = Math.Clamp(maximo, 0, LimitePrecio);
        if (maximo < minimo)
            minimo = maximo;
    }

    private void AplicarFiltros()
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
    }

    private void LimpiarFiltros()
    {
        categorias.Clear();
        autores.Clear();
        editoriales.Clear();
        idiomas.Clear();
        minimo = 0;
        maximo = LimitePrecio;
        soloActivos = false;
        AplicarFiltros();
    }

    private void QuitarCategoria(string value)
    {
        categorias.Remove(value);
        categoriasAplicadas.Remove(value);
        pagina = 1;
    }

    private void QuitarAutor(string value)
    {
        autores.Remove(value);
        autoresAplicados.Remove(value);
        pagina = 1;
    }

    private void QuitarEditorial(string value)
    {
        editoriales.Remove(value);
        editorialesAplicadas.Remove(value);
        pagina = 1;
    }

    private void QuitarIdioma(string value)
    {
        idiomas.Remove(value);
        idiomasAplicados.Remove(value);
        pagina = 1;
    }

    private void QuitarPrecio()
    {
        minimo = minimoAplicado = 0;
        maximo = maximoAplicado = LimitePrecio;
        precioFiltrado = false;
        pagina = 1;
    }

    private void QuitarSoloActivos()
    {
        soloActivos = soloActivosAplicado = false;
        pagina = 1;
    }

    private void CambiarOrden() => pagina = 1;
    private void CambiarPagina(int value) => pagina = value;
}

