using Web.Modelos;

namespace Web.Servicios;

public class CatalogoServicio
{
    private readonly HttpClientUtils api;
    public List<Referencia> Referencias { get; } = [];
    public List<string> Idiomas { get; } = [];
    public string? Error { get; private set; }
    public bool Cargando
    {
        get; private set;
    }
    public bool Inicializado
    {
        get; private set;
    }

    public CatalogoServicio(HttpClientUtils api)
    {
        this.api = api;
    }

    public async Task Inicializar()
    {
        if (Inicializado || Cargando)
            return;
        Cargando = true;
        Error = null;
        try
        {
            Referencias.Clear();
            foreach (var tipo in new[] { "autores", "categorias", "editoriales" })
            {
                var resultado = await api.Post<ResultadoPaginadoDTO<TarjetaReferenciaDTO>>(
                    tipo + "/busqueda", new FiltroReferenciaDTO
                    {
                        Nombre = "",
                        SoloActivos = true,
                        Orden = "NOMBRE_ASCENDENTE",
                        Pagina = 1,
                        TamanioPagina = int.MaxValue
                    });
                Referencias.AddRange(resultado.Items.Select(dto => Presentacion.Referencia(dto, tipo)));
            }
            var idiomas = await api.Get<ResultadoPaginadoDTO<string>>(
                $"libros/idiomas?pagina=1&tamanioPagina={int.MaxValue}");
            Idiomas.Clear();
            Idiomas.AddRange(idiomas.Items);
            Inicializado = true;
        }
        catch (InvalidOperationException e)
        {
            Error = e.Message;
        }
        Cargando = false;
    }

}
