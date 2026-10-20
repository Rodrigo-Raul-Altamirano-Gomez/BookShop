using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Catalogo
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [SupplyParameterFromQuery(Name = "q")]
    public string? Q
    {
        get; set;
    }

    [SupplyParameterFromQuery(Name = "criterio")]
    public string? Criterion
    {
        get; set;
    }
    private string Titulo => Criterion == "nuevos" ? "Libros nuevos" : Criterion == "vendidos"
        ? "Más vendidos" : "Catálogo";
    private string Sort => Criterion == "vendidos" ? "vendidos" : "reciente";
    private IEnumerable<Libro> Libros => Criterion == "nuevos" ? Estado.Libros.OrderByDescending(b => b.Id)
        .Take(16) : Estado.Libros;
}

