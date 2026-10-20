using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Recomendaciones : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;
    private bool Personal => Navegacion.ToBaseRelativePath(Navegacion.Uri).StartsWith("para-ti");
    private string Titulo => Personal ? "Para ti" : "Más populares";

    private string orden = "recomendados";
    private int pagina = 1;
    private string rutaAnterior = "";
    private List<Libro> Libros => (Personal ? Estado.Libros.Where(b => b.Activo).Skip(4).Concat(Estado
        .Libros.Where(b => b.Activo).Take(4)) : Estado.Libros.Where(b => b.Activo).Skip(8).Concat(Estado
            .Libros.Where(b => b.Activo).Take(8))).DistinctBy(b => b.Id).ToList();
    private IEnumerable<Libro> LibrosVisibles => ExploradorCatalogo.OrdenarLibros(Libros, orden).Skip((
        pagina - 1) * 12).Take(12);
    private int TotalPaginas => Math.Max(1, (Libros.Count + 11) / 12);

    protected override void OnInitialized() => Estado.Cambiado += ActualizarVista;
    protected override void OnParametersSet()
    {
        var ruta = Personal ? "para-ti" : "populares";
        if (rutaAnterior != ruta)
        {
            GuardarVista();
            rutaAnterior = ruta;
            if (Estado.EstadoVistas.TryGetValue($"recomendaciones:{ruta}", out var guardado)
                && guardado is VistaRecomendaciones vista)
            {
                orden = vista.Orden;
                pagina = vista.Pagina;
            }
            else
            {
                orden = "recomendados";
                pagina = 1;
            }
        }

        pagina = Math.Min(pagina, TotalPaginas);
    }

    private void GuardarVista()
    {
        if (rutaAnterior.Length == 0)
            return;
        Estado.EstadoVistas[$"recomendaciones:{rutaAnterior}"] = new VistaRecomendaciones
        {

            Orden = orden,

            Pagina = pagina

        };
    }

    private void ActualizarVista() => InvokeAsync(() =>
    {
        pagina = Math.Min(pagina, TotalPaginas);
        StateHasChanged();
    });
    public void Dispose()
    {
        GuardarVista();
        Estado.Cambiado -= ActualizarVista;
    }

    private class VistaRecomendaciones
    {
        public string Orden { get; set; } = "recomendados";
        public int Pagina { get; set; } = 1;
    }

    private void CambiarOrden() => pagina = 1;
    private void CambiarPagina(int value) => pagina = value;
    private void AbrirAcceso() => Estado.MostrarAcceso();
}

