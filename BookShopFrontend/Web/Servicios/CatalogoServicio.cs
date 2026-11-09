using Web.Modelos;
using Web.Modelos.DTO;

namespace Web.Servicios;

public class CatalogoServicio
{
    private readonly HttpClientUtils api;
    private readonly SesionUsuario Sesion;
    public List<Libro> Libros { get; } = [];
    public List<Referencia> Referencias { get; } = [];
    public List<string> Idiomas { get; } = [];
    public List<Resena> Resenas { get; } = [];
    public bool Cargando
    {
        get; private set;
    }
    public bool Inicializado
    {
        get; private set;
    }

    public CatalogoServicio(HttpClientUtils api, SesionUsuario sesion)
    {
        this.api = api;
        Sesion = sesion;
        Sesion.Cerrada += Limpiar;
    }

    private void Limpiar()
    {
        Resenas.Clear();

    }

    public async Task Inicializar()
    {
        if (Inicializado || Cargando)
            return;
        Cargando = true;
        try
        {
            Referencias.Clear();
            foreach (var tipo in new[] { "autores", "categorias", "editoriales" })
            {
                int pagina = 1;
                long total;
                do
                {
                    var resultado = await api.Post<ResultadoPaginadoDTO<TarjetaReferenciaDTO>>(tipo + "/busqueda", new FiltroReferenciaDTO
                    {
                        Nombre = "",
                        SoloActivos = true,
                        Orden = "NOMBRE_ASCENDENTE",
                        Pagina = pagina,
                        TamanioPagina = 12
                    });
                    Referencias.AddRange(resultado.Items.Select(d => Presentacion.Referencia(d, tipo)));
                    total = resultado.TotalResultados;
                    pagina++;
                } while ((pagina - 1) * 12 < total);
            }
            Idiomas.Clear();
            int paginaIdiomas = 1;
            long totalIdiomas;
            do
            {
                var resultado = await api.Get<ResultadoPaginadoDTO<string>>($"libros/idiomas?pagina={paginaIdiomas}&tamanioPagina=12");
                Idiomas.AddRange(resultado.Items);
                totalIdiomas = resultado.TotalResultados;
                paginaIdiomas++;
            } while ((paginaIdiomas - 1) * 12 < totalIdiomas);
            Inicializado = true;
            Sesion.Error = null;
        }
        catch (InvalidOperationException e) { Sesion.Error = e.Message; Sesion.Avisar(e.Message); }
        Cargando = false;
        Sesion.Notificar();
    }

    public void Recordar(Libro libro)
    {
        int indice = Libros.FindIndex(b => b.Id == libro.Id);
        if (indice < 0)
            Libros.Add(libro);
        else
            Libros[indice] = libro;
    }

    public static FiltroLibroDTO Filtro(int pagina = 1, string orden = "MAS_RECIENTES", int tamanio = 12, bool nuevos = false)
        => new(null, [], [], [], [], null, null, true, nuevos, orden, pagina, tamanio);
}
