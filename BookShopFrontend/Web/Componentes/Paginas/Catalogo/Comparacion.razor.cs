using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Comparacion
{
    private string? errorCarga;

    private bool cargando = true;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [SupplyParameterFromQuery(Name = "con")]
    public int IdSegundoLibro { get; set; }
    private Libro? PrimerLibro => Sesion.IdComparacion is int id ? Catalogo.Libros.FirstOrDefault(libro => libro.Id == id) : null;
    private Libro? SegundoLibro => Catalogo.Libros.FirstOrDefault(libro => libro.Id == IdSegundoLibro);
    private IEnumerable<Libro> LibrosComparados => PrimerLibro is not null && SegundoLibro is not null ? [PrimerLibro, SegundoLibro] : [];

    private Dictionary<int, ValoracionLibroDTO> valoraciones = [];
    protected override async Task OnParametersSetAsync()
    {
        cargando = true;
        errorCarga = null;
        try
        {
            if (Sesion.IdComparacion is not int primero)
                return;

            var datos = await Api.Get<List<DetalleLibroDTO>>($"libros/comparacion?primerLibro={primero}&segundoLibro={IdSegundoLibro}");
            foreach (var dto in datos)
            {
                Catalogo.Recordar(Presentacion.Libro(dto));
                valoraciones[dto.Libro.Id] = dto.Valoracion;
            }
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
    private int CantidadResenas(Libro libro)
    {
        if (!valoraciones.TryGetValue(libro.Id, out var valoracion))
            return 0;
        return (int)(valoracion.CantidadUnaEstrella + valoracion.CantidadDosEstrellas
            + valoracion.CantidadTresEstrellas + valoracion.CantidadCuatroEstrellas
            + valoracion.CantidadCincoEstrellas);
    }
    private double PromedioResenas(Libro libro)
    {
        int cantidad = CantidadResenas(libro);
        if (cantidad == 0)
            return 0;
        var valoracion = valoraciones[libro.Id];
        long puntos = valoracion.CantidadUnaEstrella + 2 * valoracion.CantidadDosEstrellas
            + 3 * valoracion.CantidadTresEstrellas + 4 * valoracion.CantidadCuatroEstrellas
            + 5 * valoracion.CantidadCincoEstrellas;
        return puntos / (double)cantidad;
    }
    private static string TextoODefecto(string valor) => string.IsNullOrWhiteSpace(valor) ? "—" : valor;
    private static string Disponibilidad(Libro libro) => libro.Activo && libro.Stock > 0 ? $"Disponible · {libro.Stock} unidades" : "No disponible";
    private void QuitarLibro(Libro libro)
    {
        Sesion.IdComparacion = libro.Id == Sesion.IdComparacion ? IdSegundoLibro : Sesion.IdComparacion;
        Sesion.Notificar();
        Navegacion.NavigateTo("/catalogo");
    }
}
