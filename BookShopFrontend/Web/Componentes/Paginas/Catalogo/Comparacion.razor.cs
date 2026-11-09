using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Comparacion
{
    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [SupplyParameterFromQuery(Name = "con")]
    public int SecondId
    {
        get; set;
    }
    private Libro? First => Sesion.IdComparacion is int id ? Catalogo.Libros.FirstOrDefault(libro => libro.Id == id) : null;
    private Libro? Second => Catalogo.Libros.FirstOrDefault(libro => libro.Id == SecondId);
    private IEnumerable<Libro> ComparedBooks => First is not null && Second is not null ? [First, Second] : [];

    private Dictionary<int, ValoracionLibroDTO> valoraciones = [];
    protected override async Task OnParametersSetAsync()
    {
        if (Sesion.IdComparacion is not int primero)
            return;
        try
        {
            var datos = await Api.Get<List<DetalleLibroDTO>>($"libros/comparacion?primerLibro={primero}&segundoLibro={SecondId}");
            foreach (var d in datos)
            {
                Catalogo.Recordar(Presentacion.Libro(d));
                valoraciones[d.Libro.Id] = d.Valoracion;
            }
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
    }
    private int CantidadResenas(Libro book) => valoraciones.TryGetValue(book.Id, out var v) ? (int)(v.CantidadUnaEstrella + v.CantidadDosEstrellas + v.CantidadTresEstrellas + v.CantidadCuatroEstrellas + v.CantidadCincoEstrellas) : 0;
    private double PromedioResenas(Libro book) => valoraciones.TryGetValue(book.Id, out var v) && CantidadResenas(book) > 0 ? (v.CantidadUnaEstrella + 2 * v.CantidadDosEstrellas + 3 * v.CantidadTresEstrellas + 4 * v.CantidadCuatroEstrellas + 5 * v.CantidadCincoEstrellas) / (double)CantidadResenas(book) : 0;
    private static string ValorODash(string value) => string.IsNullOrWhiteSpace(value) ? "—" : value;
    private static string Disponibilidad(Libro book) => book.Activo && book.Stock > 0 ? $"Disponible · {book.Stock} unidades" : "No disponible";
    private void QuitarLibro(Libro book)
    {
        Sesion.IdComparacion = book.Id == Sesion.IdComparacion ? SecondId : Sesion.IdComparacion;
        Sesion.Notificar();
        Navegacion.NavigateTo("/catalogo");
    }
}

