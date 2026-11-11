using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas;

public partial class Inicio
{
    private string? errorCarga;

    private bool cargando = true;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    private IEnumerable<Referencia> Categorias =>
        Catalogo.Referencias.Where(r => r.Tipo == "categorias" && r.Activo).Take(6);
    private IEnumerable<Referencia> Autores =>
        Catalogo.Referencias.Where(r => r.Tipo == "autores" && r.Activo).Take(6);
    private IEnumerable<Referencia> Editoriales => Catalogo.Referencias.Where(r => r.Tipo == "editoriales"
        && r.Activo).Take(6);
    private List<Libro> LibrosNuevos = [], MasVendidos = [];
    protected override async Task OnInitializedAsync()
    {
        cargando = true;
        errorCarga = null;
        try
        {
            await Catalogo.Inicializar();

            var nuevos = await Api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("libros/busqueda",
                new FiltroLibroDTO { TamanioPagina = 8, SoloNuevos = true });
            LibrosNuevos = nuevos.Items.Select(Presentacion.Libro).ToList();
            var vendidos = await Api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("libros/busqueda",
                new FiltroLibroDTO { Orden = "MAS_VENDIDOS", TamanioPagina = 8 });
            MasVendidos = vendidos.Items.Select(Presentacion.Libro).ToList();
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
}
