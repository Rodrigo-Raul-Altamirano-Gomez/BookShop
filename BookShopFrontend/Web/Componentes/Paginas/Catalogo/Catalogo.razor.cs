using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Catalogo
{
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
}

