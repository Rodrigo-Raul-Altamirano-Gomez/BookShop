using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Comparacion
{
    private string? errorCarga;

    private bool cargando = true;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [SupplyParameterFromQuery(Name = "con")]
    public int IdSegundoLibro { get; set; }
    private List<Libro> LibrosComparados = [];
    protected override async Task OnParametersSetAsync()
    {
        cargando = true;
        errorCarga = null;
        LibrosComparados.Clear();
        if (Sesion.LibroComparacion is not null)
        {
            try
            {
                int primero = Sesion.LibroComparacion.Id;
                var datos = await Api.Get<List<DetalleLibroDTO>>(
                    $"libros/comparacion?primerLibro={primero}&segundoLibro={IdSegundoLibro}");
                LibrosComparados = datos.Select(Presentacion.Libro).ToList();
            }
            catch (InvalidOperationException e)
            {
                errorCarga = e.Message;
            }
        }
        cargando = false;
    }
    private static string TextoODefecto(string valor) => string.IsNullOrWhiteSpace(valor) ? "—" : valor;
    private static string Disponibilidad(Libro libro)
    {
        if (libro.Activo && libro.Stock > 0)
            return $"Disponible · {libro.Stock} unidades";
        return "No disponible";
    }
    private void QuitarLibro(Libro libro)
    {
        if (libro.Id == Sesion.LibroComparacion?.Id)
            Sesion.LibroComparacion = LibrosComparados.FirstOrDefault(candidato => candidato.Id != libro.Id);
        Sesion.Notificar();
        Navegacion.NavigateTo("/catalogo");
    }
}
