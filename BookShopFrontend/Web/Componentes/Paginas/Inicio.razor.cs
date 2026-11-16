using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Paginas;

public partial class Inicio : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CatalogoServicio Catalogo { get; set; } = default!;
    [Inject] private SesionUsuario Sesion { get; set; } = default!;

    private IEnumerable<Referencia> Categorias =>
        Catalogo.Referencias.Where(r => r.Tipo == "categorias" && r.Activo).Take(6);
    private IEnumerable<Referencia> Autores =>
        Catalogo.Referencias.Where(r => r.Tipo == "autores" && r.Activo).Take(6);
    private IEnumerable<Referencia> Editoriales => Catalogo.Referencias.Where(r => r.Tipo == "editoriales"
        && r.Activo).Take(6);
    private List<Libro> LibrosNuevos = [], MasVendidos = [], MasPopulares = [], ParaTi = [];
    private bool mostrarParaTi;
    private string? correoCargado;

    protected override void OnInitialized() => Sesion.Cambiado += ActualizarSesion;
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
            var populares = await Api.Post<RecomendacionesDTO>("recomendaciones/populares",
                new ConsultaRecomendacionesDTO { TamanioPagina = 8 });
            MasPopulares = populares.Resultados.Items.Select(Presentacion.Libro).ToList();
            await Api.Delete($"recomendaciones/consultas/{populares.IdConsulta}");
            await CargarParaTi();
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        cargando = false;
    }

    private async Task CargarParaTi()
    {
        correoCargado = Sesion.Usuario?.Correo;
        ParaTi.Clear();
        mostrarParaTi = false;
        if (Sesion.Usuario is null)
            return;
        mostrarParaTi = await Api.Get<bool>("recomendaciones/para-ti/disponible");
        if (mostrarParaTi)
        {
            var personales = await Api.Post<RecomendacionesDTO>("recomendaciones/para-ti",
                new ConsultaRecomendacionesDTO { TamanioPagina = 8 });
            ParaTi = personales.Resultados.Items.Select(Presentacion.Libro).ToList();
            await Api.Delete($"recomendaciones/consultas/{personales.IdConsulta}");
        }
    }

    private async void ActualizarSesion()
    {
        if (correoCargado == Sesion.Usuario?.Correo)
            return;
        await InvokeAsync(async () =>
        {
            try
            {
                await CargarParaTi();
            }
            catch (InvalidOperationException e)
            {
                errorCarga = e.Message;
            }
            StateHasChanged();
        });
    }

    public void Dispose() => Sesion.Cambiado -= ActualizarSesion;
}
