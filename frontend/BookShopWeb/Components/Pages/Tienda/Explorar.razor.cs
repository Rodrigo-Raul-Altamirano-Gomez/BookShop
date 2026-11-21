using BookShopWeb.Code;
using BookShopWeb.Models;
using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Tienda;

// RF05.1: búsqueda por nombre y orden por nombre, en páginas de 24 (UX-UI §8.5)
public partial class Explorar
{
    private const int TamanoPagina = 24;

    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private IJSRuntime JS { get; set; } = default!;

    [Parameter] public string Ruta { get; set; } = "";
    [SupplyParameterFromQuery] public string? Q { get; set; }
    [SupplyParameterFromQuery] public string? Orden { get; set; }
    [SupplyParameterFromQuery] public int? Pagina { get; set; }

    private readonly TipoEntidad[] pestanas = { TipoEntidad.AUTOR, TipoEntidad.CATEGORIA, TipoEntidad.EDITORIAL };
    private TipoEntidad tipo;
    private ResultadoPagina<Entidad>? resultado;
    private string busqueda = "";
    private string orden = "nombre-asc";

    private string TextoBusqueda => "Buscar " + Entidades.Plural(tipo).ToLower() + " por nombre";
    private int TotalPaginas => Math.Max(1, (resultado!.Total + TamanoPagina - 1) / TamanoPagina);

    // La pestaña, la búsqueda, el orden y la página vienen de la URL
    protected override async Task OnParametersSetAsync()
    {
        tipo = Entidades.DesdeRuta(Ruta);
        busqueda = Q ?? "";
        orden = Orden ?? "nombre-asc";
        resultado = null;
        string consulta = $"entidades?tipo={tipo}&orden={orden}&pagina={Pagina ?? 1}&tamano={TamanoPagina}";
        if (busqueda != "")
        {
            consulta += "&q=" + Uri.EscapeDataString(busqueda);
        }
        resultado = await new HttpClientUtils<ResultadoPagina<Entidad>>().Get(consulta);
    }

    // Buscar o cambiar el orden vuelve a la página 1
    private void Buscar()
    {
        IrA(1);
    }

    private async Task CambiarPagina(int pagina)
    {
        IrA(pagina);
        await JS.InvokeVoidAsync("irArriba");
    }

    private void IrA(int pagina)
    {
        string url = "explorar/" + Ruta + "?orden=" + orden + "&pagina=" + pagina;
        if (busqueda.Trim() != "")
        {
            url += "&q=" + Uri.EscapeDataString(busqueda.Trim());
        }
        Navegacion.NavigateTo(url);
    }
}
