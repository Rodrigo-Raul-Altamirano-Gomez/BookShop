using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Catalogo
{
    [SupplyParameterFromQuery(Name = "q")]
    public string? Busqueda { get; set; }

    [SupplyParameterFromQuery(Name = "criterio")]
    public string? Criterio { get; set; }
    private string Titulo
    {
        get
        {
            if (Criterio == "nuevos")
                return "Libros nuevos";
            if (Criterio == "vendidos")
                return "Más vendidos";
            return "Catálogo";
        }
    }

    private string OrdenInicial
    {
        get
        {
            if (Criterio == "vendidos")
                return "MAS_VENDIDOS";
            return "MAS_RECIENTES";
        }
    }
}
