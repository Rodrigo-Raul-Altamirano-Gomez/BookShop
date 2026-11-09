using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas;

public partial class Inicio : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;
    private IEnumerable<Referencia> Categorias => Catalogo.Referencias.Where(r => r.Tipo == "categorias" && r
        .Activo).Take(6);
    private IEnumerable<Referencia> Autores => Catalogo.Referencias.Where(r => r.Tipo == "autores" && r
        .Activo).Take(6);
    private IEnumerable<Referencia> Editoriales => Catalogo.Referencias.Where(r => r.Tipo == "editoriales"
        && r.Activo).Take(6);
    private List<Libro> LibrosNuevos = [], MasVendidos = [];
    protected override async Task OnInitializedAsync()
    {
        cargando = true;
        errorCarga = null;
        try
        {

            Sesion.Cambiado += Actualizar;
            await Catalogo.Inicializar();

            var nuevos = await Api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("libros/busqueda", CatalogoServicio.Filtro(tamanio: 8, nuevos: true));
            LibrosNuevos = nuevos.Items.Select(Presentacion.Libro).ToList();
            var vendidos = await Api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("libros/busqueda", CatalogoServicio.Filtro(orden: "MAS_VENDIDOS", tamanio: 8));
            MasVendidos = vendidos.Items.Select(Presentacion.Libro).ToList();
            Sesion.Notificar();
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
    private void Actualizar() => InvokeAsync(StateHasChanged);
    public void Dispose() => Sesion.Cambiado -= Actualizar;
}

