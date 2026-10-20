using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Comparacion
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [SupplyParameterFromQuery(Name = "con")]
    public int SecondId
    {
        get; set;
    }
    private Libro? First => Estado.IdComparacion is int id ? Estado.BuscarLibro(id) : null;
    private Libro? Second => Estado.BuscarLibro(SecondId);
    private IEnumerable<Libro> ComparedBooks => First is not null && Second is not null ? [First, Second] : [];

    private int CantidadResenas(Libro book) => Estado.Resenas.Count(r => r.IdLibro == book.Id && r.Activo);
    private double PromedioResenas(Libro book) => Estado.Resenas.Where(r => r.IdLibro == book.Id && r
        .Activo).Select(r => (double)r.Puntuacion).DefaultIfEmpty(0).Average();
    private static string ValorODash(string value) => string.IsNullOrWhiteSpace(value) ? "—" : value;
    private static string Disponibilidad(Libro book) => book.Activo && book.Stock > 0 ? $"Disponible · {
        book.Stock} unidades" : "No disponible";
    private void QuitarLibro(Libro book)
    {
        Estado.IdComparacion = book.Id == Estado.IdComparacion ? SecondId : Estado.IdComparacion;
        Estado.Notificar();
        Navegacion.NavigateTo("/catalogo");
    }
}

